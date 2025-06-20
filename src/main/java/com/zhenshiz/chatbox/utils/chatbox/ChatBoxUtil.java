package com.zhenshiz.chatbox.utils.chatbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zhenshiz.chatbox.component.DialogBox;
import com.zhenshiz.chatbox.component.HistoricalDialogue;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.event.fabric.SkipChatEvent;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.screen.HistoricalDialogueScreen;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.*;

public class ChatBoxUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final Gson GSON =
            (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    //所有的对话框主题
    public static final Map<ResourceLocation, ChatBoxTheme> themeMap = new HashMap<>();
    //所有的对话信息
    public static final Map<ResourceLocation, Map<String, List<ChatBoxDialogues>>> dialoguesMap = new HashMap<>();
    //玩家的对话框主题
    public static ChatBoxTheme chatBoxTheme;
    //玩家的对话框信息
    public static ChatBoxScreen chatBoxScreen = new ChatBoxScreen();
    //玩家的历史对话记录
    public static HistoricalDialogueScreen historicalDialogue = new HistoricalDialogueScreen();
    //上一轮对话放的音乐
    private static ResourceLocation lastSoundResourceLocation = null;

    //跳转对话
    public static void skipDialogues(ResourceLocation dialoguesResourceLocation, String group, int index) {
        List<ChatBoxDialogues> chatBoxDialogues = dialoguesMap.get(dialoguesResourceLocation).get(group);

        if (index >= 0 && index < chatBoxDialogues.size()) {
            ChatBoxDialogues dialogue = chatBoxDialogues.get(index);
            ChatBoxDialogues.DialogBox dialogBox = dialogue.dialogBox;
            chatBoxScreen.setDialogBox(dialogBox != null ? dialogBox.setDialogBoxDialogues(chatBoxScreen.dialogBox, index, chatBoxScreen.isTranslatable) : new DialogBox())
                    .setPortrait(!CollUtil.isEmpty(dialogue.portrait) ? ChatBoxDialogues.setPortraitDialogues(dialogue.portrait, chatBoxTheme) : new ArrayList<>())
                    .setChatOptions(!CollUtil.isEmpty(dialogue.options) ? ChatBoxDialogues.Option.setChatOptionDialogues(chatBoxTheme, dialoguesResourceLocation, group, index, chatBoxScreen.isTranslatable) : new ArrayList<>());

            chatBoxScreen.dialogBox.resetTickCount();
            chatBoxScreen.dialogBox.setAllOver(false);
            if (minecraft.screen == null) {
                //清除历史记录 打开screen
                historicalDialogue = new HistoricalDialogueScreen();
                minecraft.setScreen(chatBoxScreen);
            }
            //新增聊天记录
            if (dialogBox != null && minecraft.player != null) {
                //添加历史聊天记录
                historicalDialogue.historicalDialogue.addHistoricalInfo(new HistoricalDialogue.HistoricalInfo(dialoguesResourceLocation, group, index)
                        .setName(dialogBox.name, chatBoxScreen.isTranslatable)
                        .setText(dialogBox.text, chatBoxScreen.isTranslatable)
                );
                //进入对话执行自定义指令
                if (dialogue.command != null) {
                    minecraft.player.connection.sendCommand(dialogue.command);
                }
                //播放音乐
                ResourceLocation soundResourceLocation = ResourceLocation.tryParse(dialogue.sound);
                if (soundResourceLocation != null) {
                    if (lastSoundResourceLocation != null) {
                        minecraft.getSoundManager().stop(lastSoundResourceLocation, null);
                    }
                    SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundResourceLocation);
                    if (soundEvent != null) {
                        lastSoundResourceLocation = soundResourceLocation;
                        minecraft.player.playSound(soundEvent, dialogue.volume, dialogue.pitch);
                    }
                }

                SkipChatEvent.EVENT.invoker().skipChat(chatBoxScreen, dialoguesResourceLocation, group, index);
                //NeoForge.EVENT_BUS.post(new SkipChatEvent(chatBoxScreen, dialoguesResourceLocation, group, index));
            }
        } else {
            if (minecraft.screen != null) {
                minecraft.screen.onClose();
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
                .setLogButton(chatBoxTheme.logButton.setLogButtonTheme(chatBoxScreen.logButton));
    }

    public static void setTheme(Map<ResourceLocation, String> map) {
        map.forEach((resourceLocation, str) -> {
            JsonElement jsonElement = GSON.fromJson(str, JsonElement.class);
            if (jsonElement == null) return;
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            JsonElement portraitElement = jsonObject.get("portrait");
            JsonElement chatOptionElement = jsonObject.get("option");
            JsonElement dialogBoxElement = jsonObject.get("dialogBox");
            JsonElement logButtonElement = jsonObject.get("logButton");
            Map<String, ChatBoxTheme.Portrait> portrait = new HashMap<>();
            ChatBoxTheme.Option option = new ChatBoxTheme.Option();
            ChatBoxTheme.DialogBox dialogBox = new ChatBoxTheme.DialogBox();
            ChatBoxTheme.LogButton logButton = new ChatBoxTheme.LogButton();

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
            if (logButtonElement != null) {
                logButton = GSON.fromJson(logButtonElement, ChatBoxTheme.LogButton.class);
            }

            themeMap.put(resourceLocation, new ChatBoxTheme(portrait, option, dialogBox, logButton).setDefaultValue());
        });
    }

    public static void setDialogues(Map<ResourceLocation, String> map) {
        map.forEach((resourceLocation, str) -> {
            JsonElement jsonElement = GSON.fromJson(str, JsonElement.class);
            if (jsonElement == null) return;
            JsonElement dialoguesElement = jsonElement.getAsJsonObject().get("dialogues");
            if (dialoguesElement != null) {
                Map<String, List<ChatBoxDialogues>> ChatBoxDialoguesMap = GSON.fromJson(dialoguesElement, new com.google.common.reflect.TypeToken<Map<String, List<ChatBoxDialogues>>>() {
                }.getType());
                for (Map.Entry<String, List<ChatBoxDialogues>> entry : ChatBoxDialoguesMap.entrySet()) {
                    int index = 0;
                    entry.getValue().forEach(chatBoxDialogues -> {
                        chatBoxDialogues.setDefaultValue(resourceLocation, entry.getKey(), index);
                    });
                }
                dialoguesMap.put(resourceLocation, ChatBoxDialoguesMap);
            }

            JsonElement je = jsonElement.getAsJsonObject().get("isTranslatable");
            chatBoxScreen.setIsTranslatable(je != null && je.getAsBoolean());
            je = jsonElement.getAsJsonObject().get("isEsc");
            chatBoxScreen.setIsEsc(je == null || je.getAsBoolean());
            je = jsonElement.getAsJsonObject().get("isPause");
            chatBoxScreen.setIsPause(je == null || je.getAsBoolean());
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
