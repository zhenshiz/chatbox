package com.zhenshiz.chatbox.neoforge;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ChatBox.MOD_ID)
public class Network {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ChatBox.MOD_ID);
        //s2c
        registrar.playToClient(ChatBoxPayload.ChatBoxDataToClient.TYPE, ChatBoxPayload.ChatBoxDataToClient.CODEC, ((packet, context) -> ChatBoxPayload.ChatBoxDataToClient.handleOnClient(packet)));
        registrar.playToClient(ChatBoxPayload.SyncEntityData.TYPE, ChatBoxPayload.SyncEntityData.CODEC, ((packet, context) -> ChatBoxPayload.SyncEntityData.handleOnClient(packet)));

        //c2s
        registrar.playToServer(SendClickEvent.TYPE, SendClickEvent.CODEC, ((packet, context) -> SendClickEvent.handleOnServer((ServerPlayer) context.player(), packet)));

        registrar.playBidirectional(SimplePayload.TYPE, SimplePayload.CODEC, ((packet, context) -> SimplePayload.handleOnServer((ServerPlayer) context.player(), packet)), ((packet, context) -> SimplePayload.handleOnClient(packet)));
    }
}
