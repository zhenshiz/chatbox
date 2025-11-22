package com.zhenshiz.chatbox.utils.chatbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.Codec;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.HistoricalDialogue;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.event.fabric.SkipChatEvent;
import com.zhenshiz.chatbox.mixin.EntityAccessor;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.render.ChatBoxRender;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.screen.HistoricalDialogueScreen;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatBoxUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    //所有的对话框主题
    public static final Map<ResourceLocation, ChatBoxTheme> themeMap = new HashMap<>();
    //所有的对话信息
    public static final Map<ResourceLocation, ChatBoxDialogues> dialoguesMap = new HashMap<>();
    //所有的自定义动画，并不与某个主题绑定，而是全局通用
    public static final Map<String, List<ChatBoxTheme.Portrait.CustomAnimation>> animationMap = new HashMap<>();
    //玩家的对话框主题
    public static ChatBoxTheme chatBoxTheme;
    //玩家的对话框信息
    public static ChatBoxScreen chatBoxScreen = new ChatBoxScreen();
    //玩家的历史对话记录
    public static HistoricalDialogueScreen historicalDialogue = new HistoricalDialogueScreen();
    //当前使用的对话框主题
    public static String themeResourceLocation = null;
    //文本路径
    public static ResourceLocation dialoguesResourceLocation;
    //文本分组
    public static String group;
    //文本序号
    public static Integer index;
    //对话目标实体列表
    public static List<Entity> chatTargets = new ArrayList<>();
    //对话框主题分支
    public static boolean isScreen = true;

    public static void setChatTargets(Map<Integer, CompoundTag> tags) {
        var level = minecraft.level;
        if (level == null) return;
        ChatBoxRender.lastSyncTime = level.getGameTime();
        chatTargets.clear();
        tags.forEach((id, tag) -> {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                entity.getTags().clear();
                try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(entity.problemPath(), ChatBox.LOGGER)) {
                    ValueInput input = TagValueInput.create(scopedCollector, entity.registryAccess(), tag);
                    // 读取标签和额外数据应该够了，有需要再加（不能直接entity.load(tag)）
                    input.read("Tags", Codec.STRING.sizeLimitedListOf(1024)).ifPresent(t -> entity.getTags().addAll(t));
                    ((EntityAccessor) entity).readAdditionalData(input);
                }
                chatTargets.add(entity);
            }
        });
    }

    public static void setDialoguesInfo(ResourceLocation resourceLocation, String group, Integer index) {
        if (resourceLocation != null && group != null && index != null) {
            dialoguesResourceLocation = resourceLocation;
            ChatBoxUtil.group = group;
            ChatBoxUtil.index = index;
        }
    }

    private static List<Portrait> bakePortrait(ResourceLocation newRl, String newGroup, Integer newIndex) {
        List<Portrait> portraits = chatBoxScreen.portraits;
        List<ChatBoxDialogues.Dialogues> dialogues = dialoguesMap.get(newRl).dialogues.get(newGroup);
        // 如果恰好是下一句对话，直接设置
        if (dialoguesResourceLocation != null && group != null && index != null &&
                newRl == dialoguesResourceLocation && Objects.equals(newGroup, group) && newIndex == index + 1) {
            return dialogues.get(newIndex).setPortraitDialogues(portraits);
        }
        // 否则，从当前对话开始，往前找到第一个需要清除旧立绘的对话，记录下索引。（新索引等于旧索引也如此）
        int startIndex = 0;
        for (int i = newIndex; i >= 0; i--) {
            var dialog = dialogues.get(i);
            if (dialog.clearOldPortrait) {
                startIndex = i;
                break;
            }
        }
        // 从记录索引开始，重新设置所有对话的立绘。
        for (int i = startIndex; i <= newIndex; i++) {
            var dialog = dialogues.get(i);
            dialog.setPortraitDialogues(portraits);
        }
        return portraits;
    }

    //跳转对话
    public static void skipDialogues(ResourceLocation dialoguesResourceLocation, String group, int index) {
        if (minecraft.player == null) return;

        ChatBoxDialogues chatBoxDialogues = dialoguesMap.getOrDefault(dialoguesResourceLocation, null);
        if (chatBoxDialogues == null) return;

        if (chatBoxDialogues.isScreen != null) isScreen = chatBoxDialogues.isScreen;
        String theme = chatBoxDialogues.theme;
        if (theme != null && !theme.equals(themeResourceLocation)) { //如果是同一个主题就不切换了
            toggleTheme(ResourceLocation.parse(theme));
            themeResourceLocation = theme;
        }
        List<ChatBoxDialogues.Dialogues> dialogues = chatBoxDialogues.dialogues.get(group);
        if (CollUtil.isEmpty(dialogues)) {
            ChatBox.LOGGER.warn("group \"{}\" not found or is empty!", group);
            return;
        }

        if (index >= 0 && index < dialogues.size()) {
            ChatBoxDialogues.Dialogues dialog = dialogues.get(index);
            ChatBoxDialogues.Dialogues.DialogBox dialogBox = dialog.dialogBox;
            chatBoxScreen.setDialogBox(dialogBox.setDialogBoxDialogues(chatBoxScreen.dialogBox))
                    .setVideo(dialog.video != null ? dialog.video.setVideo() : null)
                    .setPortrait(bakePortrait(dialoguesResourceLocation, group, index))
                    .setChatOptions(dialog.setChatOptionDialogues())
                    .setBackgroundImage(dialog.backgroundImage)
                    .setIsEsc(chatBoxDialogues.isEsc)
                    .setIsPause(chatBoxDialogues.isPause)
                    .setIsHistoricalSkip(chatBoxDialogues.isHistoricalSkip)
                    .setAnimationFPS(chatBoxDialogues.animationFPS)
                    .setAutoPlayTick(chatBoxDialogues.autoPlayTick)
                    .playVoice(dialog.sound)
                    // 一切就绪，再触发ON_START事件
                    .setEvents(ChatBoxDialogues.transform(dialog.renderEvents)).fireEvent("ON_START");

            if (!(minecraft.screen instanceof ChatBoxScreen || minecraft.screen instanceof HistoricalDialogueScreen)) {
                //如果不是对话框和历史记录界面跳转，就清除历史记录
                historicalDialogue = new HistoricalDialogueScreen();
            }
            //添加历史聊天记录
            historicalDialogue.historicalDialogue.addHistoricalInfo(new HistoricalDialogue.HistoricalInfo(dialoguesResourceLocation, group, index).setName(dialogBox.name).setText(dialogBox.text));
            //进入对话执行自定义指令
            if (dialog.command != null) ClientPlayNetworking.send(new SendClickEvent("COMMAND", dialog.command));

            //调试用
            //System.out.println("ChatBoxUtil.skipDialogues: " + dialoguesResourceLocation + " " + group + " " + index);
            SkipChatEvent.EVENT.invoker().skipChat(minecraft.player, dialoguesResourceLocation, group, index, chatTargets);
            ChatBoxCommandUtil.simplePayloadC2S(SimplePayload.SKIP_CHAT_C2S, StrUtil.merge(dialoguesResourceLocation.toString(), group, String.valueOf(index)));

            ChatBoxRender.isOpenChatBox = true;
            if (isScreen) {
                minecraft.setScreen(chatBoxScreen);
            } else {
                ChatBoxRender.shouldRender = true;
            }
            // 确认对话框加载完成后再设置客户端对话框信息
            setDialoguesInfo(dialoguesResourceLocation, group, index);
        } else {
            if (isScreen) {
                if (minecraft.screen != null) {
                    minecraft.screen.onClose();
                }
            } else {
                if (ChatBoxRender.isRenderChatBox()) ChatBoxRender.onClose();
            }
        }
    }

    public static void skipDialogues(ResourceLocation dialoguesResourceLocation, String dialogBlock) {
        skipDialogues(dialoguesResourceLocation, dialogBlock, 0);
    }

    public static void onCloseDialogBox() {
        if (dialoguesResourceLocation == null || group == null || minecraft.player == null) return;
        SkipChatEvent.EVENT.invoker().skipChat(minecraft.player, dialoguesResourceLocation, group, -1, chatTargets);
        ChatBoxCommandUtil.simplePayloadC2S(SimplePayload.SKIP_CHAT_C2S, StrUtil.merge(dialoguesResourceLocation.toString(), group, "-1"));
    }

    //切换对话框主题
    public static void toggleTheme(ResourceLocation themeResourceLocation) {
        chatBoxTheme = themeMap.get(themeResourceLocation);
        chatBoxScreen.setDialogBox(chatBoxTheme.dialogBox.setDialogBoxTheme(chatBoxScreen.dialogBox))
                .setFunctionalButtons(ChatBoxTheme.FunctionButton.setFunctionalButtonTheme(chatBoxTheme.functionButtons))
                .setKeyPromptRender(chatBoxTheme.keyPrompt.setKeyPromptTheme(chatBoxScreen.keyPromptRender));
    }

    public static void setTheme(Map<ResourceLocation, String> map) {
        map.forEach((resourceLocation, str) -> {
            JsonElement jsonElement = GSON.fromJson(str, JsonElement.class);
            if (jsonElement == null) return;
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            JsonElement portraitElement = jsonObject.get("portrait");
            JsonElement chatOptionElement = jsonObject.get("option");
            JsonElement dialogBoxElement = jsonObject.get("dialogBox");
            JsonElement fbElement = jsonObject.get("functionalButton");
            JsonElement keyPromptElement = jsonObject.get("keyPrompt");
            JsonElement customAnimationElement = jsonObject.get("customAnimation");
            Map<String, ChatBoxTheme.Portrait> portrait = new HashMap<>();
            ChatBoxTheme.Option option = new ChatBoxTheme.Option();
            ChatBoxTheme.DialogBox dialogBox = new ChatBoxTheme.DialogBox();
            List<ChatBoxTheme.FunctionButton> functionButton = new ArrayList<>();
            ChatBoxTheme.KeyPrompt keyPrompt = new ChatBoxTheme.KeyPrompt();
            Map<String, List<ChatBoxTheme.Portrait.CustomAnimation>> customAnimation = new HashMap<>();

            if (portraitElement != null) {
                portrait = GSON.fromJson(portraitElement, new TypeToken<Map<String, ChatBoxTheme.Portrait>>() {
                }.getType());
            }
            if (chatOptionElement != null) {
                option = GSON.fromJson(chatOptionElement, ChatBoxTheme.Option.class);
            }
            if (dialogBoxElement != null) {
                dialogBox = GSON.fromJson(dialogBoxElement, ChatBoxTheme.DialogBox.class);
            }
            if (fbElement != null) {
                functionButton = GSON.fromJson(fbElement, new TypeToken<List<ChatBoxTheme.FunctionButton>>() {}.getType());
            }
            if (keyPromptElement != null) {
                keyPrompt = GSON.fromJson(keyPromptElement, ChatBoxTheme.KeyPrompt.class);
            }
            if (customAnimationElement != null) {
                customAnimation = GSON.fromJson(customAnimationElement, new TypeToken<Map<String, List<ChatBoxTheme.Portrait.CustomAnimation>>>() {}.getType());
            }
            animationMap.putAll(customAnimation);

            themeMap.put(resourceLocation, new ChatBoxTheme(portrait, option, dialogBox, functionButton, keyPrompt).setDefaultValue());
        });
    }

    public static void setDialogues(Map<ResourceLocation, String> map) {
        map.forEach((resourceLocation, str) -> {
            JsonElement jsonElement = GSON.fromJson(str, JsonElement.class);
            if (jsonElement != null) {
                ChatBoxDialogues chatBoxDialogues = GSON.fromJson(jsonElement, new com.google.common.reflect.TypeToken<ChatBoxDialogues>() {
                }.getType());
                dialoguesMap.put(resourceLocation, chatBoxDialogues);
            }
        });
    }

    // 属性解析器映射
    private static final Map<String, Function<Entity, String>> PROPERTY_RESOLVERS = new HashMap<>();

    public static void addPropertyResolver(String key, Function<Entity, String> resolver) {
        PROPERTY_RESOLVERS.put(key, resolver);
    }

    static {
        addPropertyResolver("name", entity -> entity.getDisplayName().getString());
        addPropertyResolver("uuid", entity -> entity.getUUID().toString());
        addPropertyResolver("tags", entity -> String.join(", ", entity.getTags()));
        addPropertyResolver("health", entity -> {
            if (entity instanceof LivingEntity livingEntity) return String.valueOf(livingEntity.getHealth());
            return "0";
        });
    }

    //解析文本
    public static String parseText(String input, boolean isLineBreak) {
        if (minecraft.player != null) {
            // @s 替换成当前玩家id
            input = input.replaceAll("(?<!@)@s", Objects.requireNonNull(minecraft.player.getDisplayName()).getString());

            input = parseTargetPlaceholders(input);

            if (isLineBreak) input = input.replaceAll("\n", "");

            // 将@@ 替换为 @
            return input.replaceAll("@@", "@");
        }
        return input;
    }

    public static String parseTargetPlaceholders(String input) {
        if (chatTargets.isEmpty()) return input;
        // 匹配 <targetN.property> 或 <targetN> 格式的占位符
        Pattern pattern = Pattern.compile("<target(\\d+)(\\.(\\w+))?>");
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;

        while (matcher.find()) {
            // 追加匹配前的文本
            sb.append(input, lastIndex, matcher.start());
            lastIndex = matcher.end();
            try {
                // 获取目标索引
                int index = Integer.parseInt(matcher.group(1)) - 1; // 转换为0-based索引
                // 获取属性名，如果没有指定则默认为"name"
                String property = matcher.group(3) != null ? matcher.group(3) : "name";
                // 检查索引是否有效
                if (index >= 0 && index < chatTargets.size()) {
                    Entity target = chatTargets.get(index);
                    // 获取属性解析器
                    Function<Entity, String> resolver = PROPERTY_RESOLVERS.getOrDefault(property, null);
                    if (resolver != null) {
                        // 应用解析器获取属性值
                        sb.append(resolver.apply(target));
                    } else {
                        // 如果没有对应的解析器，保留占位符
                        sb.append(matcher.group());
                    }
                } else {
                    // 如果索引无效，保留占位符
                    sb.append(matcher.group());
                }
            } catch (NumberFormatException e) {
                // 如果解析索引失败，保留占位符
                sb.append(matcher.group());
            }
        }
        // 追加剩余文本
        sb.append(input.substring(lastIndex));
        return sb.toString();
    }
}
