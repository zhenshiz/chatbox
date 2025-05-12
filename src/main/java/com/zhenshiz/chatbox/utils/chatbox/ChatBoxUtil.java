package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.component.DialogBox;
import com.zhenshiz.chatbox.component.HistoricalDialogue;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.screen.HistoricalDialogueScreen;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class ChatBoxUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();
    //各个玩家的对话框主题
    public static final Map<UUID, ResourceLocation> playerChatBoxTheme = new HashMap<>();
    //各个玩家的对话框信息
    public static final Map<UUID, ChatBoxScreen> chatBoxScreens = new HashMap<>();
    //各个玩家的历史对话记录
    public static final Map<UUID, HistoricalDialogueScreen> historicalDialogue = new HashMap<>();

    //跳转对话
    public static void skipDialogues(UUID uuid, ResourceLocation dialoguesResourceLocation, String dialogBlock, int index) {
        List<ChatBoxDialogues> chatBoxDialogues = ChatBoxDialoguesLoader.INSTANCE.getDialogues(dialoguesResourceLocation).get(dialogBlock);
        ChatBoxScreen chatBoxScreen = chatBoxScreens.get(uuid);
        ResourceLocation themeResourceLocation = playerChatBoxTheme.get(uuid);

        if (index >= 0 && index < chatBoxDialogues.size()) {
            ChatBoxDialogues dialogue = chatBoxDialogues.get(index);
            ChatBoxDialogues.DialogBox dialogBox = dialogue.dialogBox;
            chatBoxScreen.setDialogBox(dialogBox != null ? dialogBox.setDialogBoxDialogues(chatBoxScreen.dialogBox, index) : new DialogBox())
                    .setPortrait(!CollUtil.isEmpty(dialogue.portrait) ? ChatBoxDialogues.Portrait.setPortraitDialogues(dialogue.portrait, themeResourceLocation) : new ArrayList<>())
                    .setChatOptions(!CollUtil.isEmpty(dialogue.options) ? ChatBoxDialogues.Option.setChatOptionDialogues(themeResourceLocation, dialoguesResourceLocation, dialogBlock, index) : new ArrayList<>());

            chatBoxScreen.dialogBox.resetTickCount();
            chatBoxScreen.dialogBox.setAllOver(false);
            if (minecraft.screen == null) {
                //清除历史记录 打开screen
                historicalDialogue.put(uuid, new HistoricalDialogueScreen());
                minecraft.setScreen(chatBoxScreen);
            }
            //新增聊天记录
            HistoricalDialogueScreen historicalDialogueScreen = historicalDialogue.get(uuid);
            if (dialogBox != null) {
                historicalDialogueScreen.historicalDialogue.addHistoricalInfo(new HistoricalDialogue.HistoricalInfo(dialoguesResourceLocation, dialogBlock, index)
                        .setName(dialogBox.name, dialogBox.isTranslatable)
                        .setText(dialogBox.text,dialogBox.isTranslatable)
                );
            }
        } else {
            if (minecraft.screen != null) {
                minecraft.screen.onClose();
            }
        }

        if (uuid != null) chatBoxScreens.put(uuid, chatBoxScreen);
    }

    public static void skipDialogues(UUID uuid, ResourceLocation dialoguesResourceLocation, String dialogBlock) {
        skipDialogues(uuid, dialoguesResourceLocation, dialogBlock, 0);
    }

    //切换对话框主题
    public static void toggleTheme(UUID uuid, ResourceLocation themeResourceLocation) {
        ChatBoxScreen chatBoxScreen = chatBoxScreens.get(uuid);
        ChatBoxTheme theme = ChatBoxThemeLoader.INSTANCE.getTheme(themeResourceLocation);
        chatBoxScreen.setDialogBox(theme.dialogBox.setDialogBoxTheme(chatBoxScreen.dialogBox))
                .setLogButton(theme.logButton.setLogButtonTheme(chatBoxScreen.logButton));
        if (uuid != null) {
            chatBoxScreens.put(uuid, chatBoxScreen);
            playerChatBoxTheme.put(uuid, themeResourceLocation);
        }
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
