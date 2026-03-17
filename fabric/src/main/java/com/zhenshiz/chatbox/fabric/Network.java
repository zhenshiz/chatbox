package com.zhenshiz.chatbox.fabric;

import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Network {

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ChatBoxPayload.ChatBoxDataToClient.TYPE, ChatBoxPayload.ChatBoxDataToClient.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ChatBoxPayload.SyncEntityData.TYPE, ChatBoxPayload.SyncEntityData.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SimplePayload.TYPE, SimplePayload.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(SimplePayload.TYPE, SimplePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SendClickEvent.TYPE, SendClickEvent.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SendClickEvent.TYPE, ((packet, context) -> SendClickEvent.handleOnServer(context.player(), packet)));
        ServerPlayNetworking.registerGlobalReceiver(SimplePayload.TYPE, ((packet, context) -> SimplePayload.handleOnServer(context.player(), packet)));
    }
}
