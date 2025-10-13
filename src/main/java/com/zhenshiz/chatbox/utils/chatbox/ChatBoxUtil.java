package com.zhenshiz.chatbox.utils.chatbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.component.HistoricalDialogue;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.event.fabric.SkipChatEvent;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.render.ChatBoxRender;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.screen.HistoricalDialogueScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.*;

public class ChatBoxUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();
    public static final Gson GSON =
            (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
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
    //上一轮对话放的音乐
    public static ResourceLocation lastSoundResourceLocation = null;
    //当前使用的对话框主题
    public static String themeResourceLocation = null;
    //文本路径
    public static ResourceLocation dialoguesResourceLocation;
    //文本分组
    public static String group;
    //文本序号
    public static Integer index;
    //对话框主题分支
    public static boolean isScreen = true;

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

        ChatBoxDialogues chatBoxDialogues = dialoguesMap.get(dialoguesResourceLocation);
        Boolean isTranslatable = chatBoxDialogues.isTranslatable;
        if (chatBoxDialogues.isScreen != null) isScreen = chatBoxDialogues.isScreen;
        String theme = chatBoxDialogues.theme;
        if (theme != null && !theme.equals(themeResourceLocation)) { //如果是同一个主题就不切换了
            toggleTheme(ResourceLocation.parse(theme));
            themeResourceLocation = theme;
        }
        List<ChatBoxDialogues.Dialogues> dialogues = chatBoxDialogues.dialogues.get(group);

        if (index >= 0 && index < dialogues.size()) {
            ChatBoxDialogues.Dialogues dialog = dialogues.get(index);
            ChatBoxDialogues.Dialogues.DialogBox dialogBox = dialog.dialogBox;
            chatBoxScreen.setDialogBox(dialogBox.setDialogBoxDialogues(chatBoxScreen.dialogBox, isTranslatable))
                    .setVideo(dialog.video != null ? dialog.video.setVideo() : null)
                    .setPortrait(bakePortrait(dialoguesResourceLocation, group, index))
                    .setChatOptions(dialog.setChatOptionDialogues(isTranslatable))
                    .setBackgroundImage(dialog.backgroundImage)
                    .setIsTranslatable(isTranslatable)
                    .setIsEsc(chatBoxDialogues.isEsc)
                    .setIsPause(chatBoxDialogues.isPause)
                    .setIsHistoricalSkip(chatBoxDialogues.isHistoricalSkip);

            chatBoxScreen.dialogBox.resetTickCount();
            chatBoxScreen.dialogBox.setAllOver(false);
            if (!(minecraft.screen instanceof ChatBoxScreen || minecraft.screen instanceof HistoricalDialogueScreen)) {
                //如果不是对话框和历史记录界面跳转，就清除历史记录
                historicalDialogue = new HistoricalDialogueScreen();
            }
            //添加历史聊天记录
            historicalDialogue.historicalDialogue.addHistoricalInfo(new HistoricalDialogue.HistoricalInfo(dialoguesResourceLocation, group, index)
                    .setName(dialogBox.name, isTranslatable)
                    .setText(dialogBox.text, isTranslatable)
            );
            //进入对话执行自定义指令
            if (dialog.command != null) ClientPlayNetworking.send(new SendClickEvent("COMMAND", dialog.command));
            //播放音乐
            ResourceLocation soundResourceLocation = ResourceLocation.tryParse(dialog.sound);
            //如果新的一句话没有音效，根据配置决定是否中断上一句话的音效
            if (!ChatBoxClient.conf.soundInterruptionEnabled && Objects.equals(dialog.sound, "")) soundResourceLocation = null;
            if (soundResourceLocation != null) {
                if (lastSoundResourceLocation != null) {
                    minecraft.getSoundManager().stop(lastSoundResourceLocation, null);
                }
                SoundEvent soundEvent = Holder.direct(SoundEvent.createVariableRangeEvent(soundResourceLocation)).value();
                lastSoundResourceLocation = soundResourceLocation;
                minecraft.player.playSound(soundEvent, dialog.volume, dialog.pitch);
            }

            //调试用
            //System.out.println("ChatBoxUtil.skipDialogues: " + dialoguesResourceLocation + " " + group + " " + index);
            SkipChatEvent.EVENT.invoker().skipChat(chatBoxScreen, dialoguesResourceLocation, group, index);
            //NeoForge.EVENT_BUS.post(new SkipChatEvent(chatBoxScreen, dialoguesResourceLocation, group, index));

            if (isScreen) {
                minecraft.setScreen(chatBoxScreen);
            } else {
                ChatBoxRender.isOpenChatBox = true;
            }
            // 确认对话框加载完成后再设置客户端对话框信息
            setDialoguesInfo(dialoguesResourceLocation, group, index);
        } else {
            if (isScreen) {
                if (minecraft.screen != null) {
                    minecraft.screen.onClose();
                }
            } else {
                ChatBoxRender.onClose();
            }
        }
    }

    public static void skipDialogues(ResourceLocation dialoguesResourceLocation, String dialogBlock) {
        skipDialogues(dialoguesResourceLocation, dialogBlock, 0);
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

    //解析文本
    public static String parseText(String input, boolean isLineBreak) {
        if (minecraft.player != null) {
            // @s 替换成当前玩家id
            input = input.replaceAll("(?<!@)@s", Objects.requireNonNull(minecraft.player.getDisplayName()).getString());

            if (isLineBreak) input = input.replaceAll("\n", "");

            // 将@@ 替换为 @
            return input.replaceAll("@@", "@");
        }
        return input;
    }
}
