package com.zhenshiz.chatbox.network;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ChatBox.MOD_ID)
public class ChatBoxNetWork {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ChatBox.MOD_ID);
        //s2c
        registrar.playToClient(ChatBoxPayload.ChatBoxDataToClient.TYPE, ChatBoxPayload.ChatBoxDataToClient.CODEC, ChatBoxPayload.ChatBoxDataToClient::execute);
        registrar.playToClient(ChatBoxPayload.SyncEntityData.TYPE, ChatBoxPayload.SyncEntityData.CODEC, ChatBoxPayload.SyncEntityData::execute);

        //c2s
        registrar.playToServer(SendClickEvent.TYPE, SendClickEvent.CODEC, SendClickEvent::execute);

        registrar.playBidirectional(SimplePayload.TYPE, SimplePayload.CODEC, SimplePayload::execute);
    }
}
