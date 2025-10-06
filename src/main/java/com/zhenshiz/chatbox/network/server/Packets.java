package com.zhenshiz.chatbox.network.server;

import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Packets {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.OpenScreenPayload.TYPE, ChatBoxPayload.OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.OpenChatBox.TYPE, ChatBoxPayload.OpenChatBox.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.ToggleTheme.TYPE, ChatBoxPayload.ToggleTheme.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.AllChatBoxThemeToClient.TYPE, ChatBoxPayload.AllChatBoxThemeToClient.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.AllChatBoxDialoguesToClient.TYPE, ChatBoxPayload.AllChatBoxDialoguesToClient.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.NextDialoguePayload.TYPE, ChatBoxPayload.NextDialoguePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.AutoPlayPayload.TYPE, ChatBoxPayload.AutoPlayPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(SendClickEvent.TYPE, SendClickEvent.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SendClickEvent.TYPE, SendClickEvent::execute);
    }
}