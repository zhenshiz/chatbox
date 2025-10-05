package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.*;

public class ChatBoxCommandUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();

    public static void serverToggleTheme(ServerPlayer player, ResourceLocation theme) {
        if (player != null) ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.ToggleTheme(theme));
    }

    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index) {
        if (player != null) ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.OpenScreen(dialogues, group, index));
    }

    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group) {
        serverSkipDialogues(player, dialogues, group, 0);
    }

    public static void serverOpenChatBox(ServerPlayer player) {
        if (player != null) ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.OpenChatBox());
    }

    public static void clientToggleTheme(ResourceLocation theme) {
        toggleTheme(theme);
        themeResourceLocation = theme.toString();
    }

    public static void clientSkipDialogues(ResourceLocation dialogues, String group, Integer index) {
        skipDialogues(dialogues, group, index);
    }

    public static void clientSkipDialogues(ResourceLocation dialogues, String group) {
        clientSkipDialogues(dialogues, group, 0);
    }

    public static void clientOpenChatBox() {
        if (minecraft.player != null && dialoguesResourceLocation != null && group != null && index != null) {
            skipDialogues(dialoguesResourceLocation, group, index);
        }
    }

    public static void clientNextDialogue() {
        chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
    }

    public static void clientAutoPlay(boolean autoPlay) {
        chatBoxScreen.autoPlay = autoPlay;
    }

}
