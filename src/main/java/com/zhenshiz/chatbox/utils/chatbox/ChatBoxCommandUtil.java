package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.component.DialogBox;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ChatBoxCommandUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();
    public static String themeResourceLocation = null;

    public static void serverToggleTheme(ServerPlayer player, ResourceLocation theme) {
        if (player != null) player.connection.send(new ChatBoxPayload.ToggleTheme(theme));
    }

    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index) {
        if (player != null) player.connection.send(new ChatBoxPayload.OpenScreenPayload(dialogues, group, index));
    }

    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group) {
        serverSkipDialogues(player, dialogues, group, 0);
    }

    public static void openChatBox(ServerPlayer player) {
        if (player != null) player.connection.send(new ChatBoxPayload.OpenChatBox());
    }

    public static void clientToggleTheme(ResourceLocation theme) {
        ChatBoxUtil.toggleTheme(theme);
        themeResourceLocation = theme.toString();
    }

    public static void clientSkipDialogues(ResourceLocation dialogues, String group, Integer index) {
        ChatBoxUtil.skipDialogues(dialogues, group, index);
    }

    public static void clientSkipDialogues(ResourceLocation dialogues, String group) {
        ChatBoxUtil.skipDialogues(dialogues, group, 0);
    }

    public static void clientOpenChatBox() {
        if (minecraft.player != null) {
            DialogBox dialogBox = ChatBoxUtil.chatBoxScreen.dialogBox;
            ResourceLocation dialoguesResourceLocation = dialogBox.dialoguesResourceLocation;
            String group = dialogBox.group;
            Integer index = dialogBox.index;
            if (dialoguesResourceLocation != null && group != null && index != null) {
                ChatBoxUtil.skipDialogues(dialoguesResourceLocation, group, index);
            }
        }
    }
}
