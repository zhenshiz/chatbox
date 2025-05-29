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
import com.zhenshiz.chatbox.event.KubeJSEvents;
import com.zhenshiz.chatbox.event.SkipChatEvent;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.screen.HistoricalDialogueScreen;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    //跳转对话
    public static void skipDialogues(ResourceLocation dialoguesResourceLocation, String group, int index) {
        List<ChatBoxDialogues> chatBoxDialogues = dialoguesMap.get(dialoguesResourceLocation).get(group);

        if (index >= 0 && index < chatBoxDialogues.size()) {
            ChatBoxDialogues dialogue = chatBoxDialogues.get(index);
            ChatBoxDialogues.DialogBox dialogBox = dialogue.dialogBox;
            chatBoxScreen.setDialogBox(dialogBox != null ? dialogBox.setDialogBoxDialogues(chatBoxScreen.dialogBox, index) : new DialogBox())
                    .setPortrait(!CollUtil.isEmpty(dialogue.portrait) ? ChatBoxDialogues.Portrait.setPortraitDialogues(dialogue.portrait, chatBoxTheme) : new ArrayList<>())
                    .setChatOptions(!CollUtil.isEmpty(dialogue.options) ? ChatBoxDialogues.Option.setChatOptionDialogues(chatBoxTheme, dialoguesResourceLocation, group, index) : new ArrayList<>());

            chatBoxScreen.dialogBox.resetTickCount();
            chatBoxScreen.dialogBox.setAllOver(false);
            if (minecraft.screen == null) {
                //清除历史记录 打开screen
                historicalDialogue = new HistoricalDialogueScreen();
                minecraft.setScreen(chatBoxScreen);
            }
            //新增聊天记录
            if (dialogBox != null) {
                historicalDialogue.historicalDialogue.addHistoricalInfo(new HistoricalDialogue.HistoricalInfo(dialoguesResourceLocation, group, index)
                        .setName(dialogBox.name, dialogBox.isTranslatable)
                        .setText(dialogBox.text, dialogBox.isTranslatable)
                );
                ResourceLocation soundResourceLocation = ResourceLocation.tryParse(dialogue.sound);
                if (minecraft.player != null && soundResourceLocation != null) {
                    SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundResourceLocation);
                    if (soundEvent != null) {
                        minecraft.player.playSound(soundEvent, dialogue.volume, dialogue.pitch);
                    }
                }
                KubeJSEvents.SKIP_CHAT.post(new SkipChatEvent(dialoguesResourceLocation, group, index));
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
        });
    }

    //解析文本
    public static String parseText(String input, boolean isLineBreak) {
        if (minecraft.player != null) {
            // @s 替换成当前玩家id
            input = input.replaceAll("(?<!@)@s", minecraft.player.getDisplayName().getString());

            if (isLineBreak) input = input.replaceAll("\n", "");

            // 将@@ 替换为 @
            return input.replaceAll("@@", "@");
        }
        return input;
    }
}
