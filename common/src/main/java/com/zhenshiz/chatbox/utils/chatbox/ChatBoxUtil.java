package com.zhenshiz.chatbox.utils.chatbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.data.Keyframe;
import com.zhenshiz.chatbox.mixin.EntityAccessor;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.render.ChatBoxRender;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.screen.HistoricalDialogueScreen;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.*;

public class ChatBoxUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    //所有的对话框主题
    public static final Map<Identifier, ChatBoxTheme> themeMap = new HashMap<>();
    //所有的对话信息
    public static final Map<Identifier, ChatBoxDialogues> dialoguesMap = new HashMap<>();
    //所有的自定义动画，并不与某个主题绑定，而是全局通用
    public static final Map<String, List<Keyframe>> animationMap = new HashMap<>();
    //玩家的对话框主题
    public static ChatBoxTheme chatBoxTheme;
    //玩家的对话框信息
    public static ChatBoxScreen chatBoxScreen = new ChatBoxScreen();
    //玩家的历史对话记录
    public static HistoricalDialogueScreen historicalDialogue = new HistoricalDialogueScreen();
    //当前使用的对话框主题
    public static String themeIdentifier = null;
    //文本路径
    public static Identifier dialoguesIdentifier;
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
                entity.entityTags().clear();
                try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(entity.problemPath(), ChatBox.LOGGER)) {
                    ValueInput input = TagValueInput.create(scopedCollector, entity.registryAccess(), tag);
                    // 读取标签和额外数据应该够了，有需要再加（不能直接entity.load(tag)）
                    input.read("Tags", Codec.STRING.sizeLimitedListOf(1024)).ifPresent(t -> entity.entityTags().addAll(t));
                    ((EntityAccessor) entity).readAdditionalData(input);
                }
                chatTargets.add(entity);
            }
        });
    }

    public static void setDialoguesInfo(Identifier identifier, String group, Integer index) {
        if (identifier != null && group != null && index != null) {
            dialoguesIdentifier = identifier;
            ChatBoxUtil.group = group;
            ChatBoxUtil.index = index;
        }
    }

    private static List<Portrait<?>> bakePortrait(Identifier newRl, String newGroup, Integer newIndex) {
        List<Portrait<?>> portraits = chatBoxScreen.portraits;
        List<ChatBoxDialogues.Dialogues> dialogues = dialoguesMap.get(newRl).dialogues.get(newGroup);
        // 如果恰好是下一句对话，直接设置
        if (dialoguesIdentifier != null && group != null && index != null &&
                newRl == dialoguesIdentifier && Objects.equals(newGroup, group) && newIndex == index + 1) {
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
    public static void skipDialogues(Identifier dialoguesIdentifier, String group, int index) {
        if (minecraft.player == null) return;

        ChatBoxDialogues chatBoxDialogues = dialoguesMap.getOrDefault(dialoguesIdentifier, null);
        if (chatBoxDialogues == null) return;

        if (chatBoxDialogues.isScreen != null) isScreen = chatBoxDialogues.isScreen;
        String theme = chatBoxDialogues.theme;
        if (theme != null && !theme.equals(themeIdentifier)) { //如果是同一个主题就不切换了
            toggleTheme(Identifier.parse(theme));
            themeIdentifier = theme;
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
                    .setPortrait(bakePortrait(dialoguesIdentifier, group, index))
                    .setChatOptions(dialog.setChatOptionDialogues())
                    .setBackgroundImage(dialog.backgroundImage)
                    .setIsEsc(chatBoxDialogues.isEsc)
                    .setIsPause(chatBoxDialogues.isPause)
                    .setIsHistoricalSkip(chatBoxDialogues.isHistoricalSkip)
                    .setAnimationFPS(chatBoxDialogues.animationFPS)
                    .setAutoPlayTick(chatBoxDialogues.autoPlayTick)
                    .playVoice(dialog.sound)
                    // 一切就绪，再触发ON_START事件
                    .setEvents(dialog.renderEvents).fireEvent("ON_START");

            if (!(minecraft.screen instanceof ChatBoxScreen || minecraft.screen instanceof HistoricalDialogueScreen)) {
                //如果不是对话框和历史记录界面跳转，就清除历史记录
                historicalDialogue = new HistoricalDialogueScreen();
            }
            //添加历史聊天记录
            historicalDialogue.historicalDialogue.addHistoricalInfo(dialoguesIdentifier, group, index, dialogBox.name, dialogBox.text);

            //调试用
            //System.out.println("ChatBoxUtil.skipDialogues: " + dialoguesIdentifier + " " + group + " " + index);
            ChatBox.PLATFORM.postSkipChatEvent(minecraft.player, dialoguesIdentifier, group, index, chatTargets);
            ChatBoxCommandUtil.simplePayloadC2S(SimplePayload.SKIP_CHAT_C2S, StrUtil.merge(dialoguesIdentifier.toString(), group, String.valueOf(index)));

            ChatBoxRender.isOpenChatBox = true;
            if (isScreen) {
                minecraft.setScreen(chatBoxScreen);
            } else {
                // 防止进入两种模式的叠加态
                if (minecraft.screen instanceof ChatBoxScreen) minecraft.setScreen(null);
                ChatBoxRender.shouldRender = true;
            }
            // 确认对话框加载完成后再设置客户端对话框信息
            setDialoguesInfo(dialoguesIdentifier, group, index);
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

    public static void skipDialogues(Identifier dialoguesIdentifier, String dialogBlock) {
        skipDialogues(dialoguesIdentifier, dialogBlock, 0);
    }

    public static void onCloseDialogBox() {
        if (dialoguesIdentifier == null || group == null || minecraft.player == null) return;
        ChatBox.PLATFORM.postSkipChatEvent(minecraft.player, dialoguesIdentifier, group, -1, chatTargets);
        ChatBoxCommandUtil.simplePayloadC2S(SimplePayload.SKIP_CHAT_C2S, StrUtil.merge(dialoguesIdentifier.toString(), group, "-1"));
    }

    //切换对话框主题
    public static void toggleTheme(Identifier themeIdentifier) {
        chatBoxTheme = themeMap.get(themeIdentifier);
        chatBoxScreen.setDialogBox(chatBoxTheme.dialogBox.setDialogBoxTheme(chatBoxScreen.dialogBox))
                .setFunctionalButtons(ChatBoxTheme.setButtonTheme(chatBoxTheme.functionalButton))
                .setKeyPromptRender(chatBoxTheme.keyPrompt.setKeyPromptTheme(chatBoxScreen.keyPromptRender));
    }

    public static void setTheme(Map<Identifier, String> map) {
        map.forEach((identifier, str) -> {
            try {
                ChatBoxTheme chatBoxTheme = GSON.fromJson(str, ChatBoxTheme.class).setDefaultValue();
                themeMap.put(identifier, chatBoxTheme);
                animationMap.putAll(chatBoxTheme.customAnimation);
            } catch (JsonSyntaxException e) {
                ChatBox.LOGGER.error("Error parsing theme for {}: {}", identifier, e.getMessage());
            }
        });
    }

    public static void setDialogues(Map<Identifier, String> map) {
        map.forEach((identifier, str) -> {
            try {
                ChatBoxDialogues chatBoxDialogues = GSON.fromJson(str, ChatBoxDialogues.class);
                dialoguesMap.put(identifier, chatBoxDialogues);
            } catch (JsonSyntaxException e) {
                ChatBox.LOGGER.error("Error parsing dialogues for {}: {}", identifier, e.getMessage());
            }
        });
    }

    //解析文本
    public static String parseText(String input, boolean isLineBreak) {
        if (minecraft.player != null) {
            // @s 替换成当前玩家id
            input = input.replaceAll("(?<!@)@s", Objects.requireNonNull(minecraft.player.getDisplayName()).getString());

            input = PlaceholderUtil.parseTargetPlaceholders(chatTargets, input);

            if (!isLineBreak) input = input.replaceAll("\n", "");

            // 将@@ 替换为 @
            return input.replaceAll("@@", "@");
        }
        return input;
    }
}
