package com.zhenshiz.chatbox.network.server;

import com.zhenshiz.chatbox.network.c2s.SendCommandPayload;
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
        PayloadTypeRegistry.playC2S().register(SendCommandPayload.TYPE, SendCommandPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SendCommandPayload.TYPE, SendCommandPayload::execute);
    }
}