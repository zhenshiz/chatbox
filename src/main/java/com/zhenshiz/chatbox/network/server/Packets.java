package com.zhenshiz.chatbox.network.server;

import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Packets {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.OpenScreen.TYPE, ChatBoxPayload.OpenScreen.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.AllChatBoxThemeToClient.TYPE, ChatBoxPayload.AllChatBoxThemeToClient.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.AllChatBoxDialoguesToClient.TYPE, ChatBoxPayload.AllChatBoxDialoguesToClient.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.SyncEntityData.TYPE, ChatBoxPayload.SyncEntityData.CODEC);
        PayloadTypeRegistry.playS2C().register(SimplePayload.TYPE, SimplePayload.CODEC);

        PayloadTypeRegistry.playC2S().register(SimplePayload.TYPE, SimplePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SendClickEvent.TYPE, SendClickEvent.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SimplePayload.TYPE, SimplePayload::execute);
        ServerPlayNetworking.registerGlobalReceiver(SendClickEvent.TYPE, SendClickEvent::execute);
    }
}