package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.api.ChatOptionClickEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.*;

public class ChatBoxCommandUtil {

    public static void serverToggleTheme(ServerPlayer player, ResourceLocation theme) {
        if (player != null) ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.SimplePayload("set_theme", theme.toString()));
    }

    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index) {
        if (player != null) ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.OpenScreen(dialogues, group, index));
    }

    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group) {
        serverSkipDialogues(player, dialogues, group, 0);
    }

    public static void serverOpenChatBox(ServerPlayer player) {
        if (player != null) ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.SimplePayload("open_dialog", ""));
    }

    public static void clientToggleTheme(String theme) {
        toggleTheme(new ResourceLocation(theme));
        themeResourceLocation = theme;
    }

    public static void clientSkipDialogues(ResourceLocation dialogues, String group, Integer index) {
        skipDialogues(dialogues, group, index);
    }

    public static void clientSkipDialogues(ResourceLocation dialogues, String group) {
        clientSkipDialogues(dialogues, group, 0);
    }

    public static void clientOpenChatBox() {
        if (dialoguesResourceLocation != null && group != null && index != null) {
            skipDialogues(dialoguesResourceLocation, group, index);
        }
    }

    public static void clientNextDialogue() {
        chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
    }

    public static void clientAutoPlay(boolean autoPlay) {
        chatBoxScreen.autoPlay = autoPlay;
    }

    public static void registerClickEvent(String type, Consumer<String> executeOnClient, boolean shouldExecuteOnServer, BiConsumer<ServerPlayer, String> executeOnServer) {
        ChatOptionClickEvent.registerClickEvent(type, executeOnClient, () -> shouldExecuteOnServer, executeOnServer);
    }

    public static void clientSetIsScreen(boolean isScreen) {
        ChatBoxUtil.isScreen = isScreen;
    }

}
