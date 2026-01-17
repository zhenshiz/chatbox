package com.zhenshiz.chatbox.fabric;

import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Network {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.ChatBoxDataToClient.TYPE, ChatBoxPayload.ChatBoxDataToClient.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.SyncEntityData.TYPE, ChatBoxPayload.SyncEntityData.CODEC);
        PayloadTypeRegistry.playS2C().register(SimplePayload.TYPE, SimplePayload.CODEC);

        PayloadTypeRegistry.playC2S().register(SimplePayload.TYPE, SimplePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SendClickEvent.TYPE, SendClickEvent.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SendClickEvent.TYPE, ((packet, context) -> SendClickEvent.handleOnServer(context.player(), packet)));
        ServerPlayNetworking.registerGlobalReceiver(SimplePayload.TYPE, ((packet, context) -> SimplePayload.handleOnServer(context.player(), packet)));
    }
}
