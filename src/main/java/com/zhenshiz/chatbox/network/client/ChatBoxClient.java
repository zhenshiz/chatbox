package com.zhenshiz.chatbox.network.client;

import com.zhenshiz.chatbox.component.DialogBox;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class ChatBoxClient {
    private static final Minecraft minecraft = Minecraft.getInstance();

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.OpenScreenPayload.TYPE, ChatBoxPayload.OpenScreenPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.OpenScreenPayload.TYPE,
                (payload, context) -> ChatBoxUtil.skipDialogues(payload.dialogues(), payload.group(), payload.index()));

        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.OpenChatBox.TYPE, ChatBoxPayload.OpenChatBox.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.OpenChatBox.TYPE,
                (payload, context) -> {
                    if (minecraft.player != null) {
                        DialogBox dialogBox = ChatBoxUtil.chatBoxScreen.dialogBox;
                        ResourceLocation dialoguesResourceLocation = dialogBox.dialoguesResourceLocation;
                        String group = dialogBox.group;
                        Integer index = dialogBox.index;
                        if (dialoguesResourceLocation != null && group != null && index != null) {
                            ChatBoxUtil.skipDialogues(dialoguesResourceLocation, group, index);
                        }
                    }
                });

        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.ToggleTheme.TYPE, ChatBoxPayload.ToggleTheme.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.ToggleTheme.TYPE,
                (payload, context) -> ChatBoxUtil.toggleTheme(payload.theme()));

        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.AllChatBoxThemeToClient.TYPE, ChatBoxPayload.AllChatBoxThemeToClient.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.AllChatBoxThemeToClient.TYPE,
                (payload, context) -> ChatBoxUtil.setTheme(payload.themeMap()));

        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.AllChatBoxDialoguesToClient.TYPE, ChatBoxPayload.AllChatBoxDialoguesToClient.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.AllChatBoxDialoguesToClient.TYPE,
                (payload, context) -> ChatBoxUtil.setDialogues(payload.dialoguesMap()));

    }
}