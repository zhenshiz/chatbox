package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ChatBoxCommandUtil {

    public static void toggleTheme(ServerPlayer player, ResourceLocation theme) {
        if (player != null) player.connection.send(new ChatBoxPayload.ToggleTheme(theme));
    }

    public static void skipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index) {
        if (player != null) player.connection.send(new ChatBoxPayload.OpenScreenPayload(dialogues, group, index));
    }

    public static void skipDialogues(ServerPlayer player, ResourceLocation dialogues, String group) {
        skipDialogues(player, dialogues, group, 0);
    }

    public static void openChatBox(ServerPlayer player) {
        if (player != null) player.connection.send(new ChatBoxPayload.OpenChatBox());
    }
}
