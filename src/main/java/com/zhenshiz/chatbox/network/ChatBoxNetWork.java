package com.zhenshiz.chatbox.network;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.network.c2s.ServerChatBoxPayload;
import com.zhenshiz.chatbox.network.s2c.ClientChatBoxPayload;
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
        registrar.playToClient(ClientChatBoxPayload.ChatBoxDataToClient.TYPE, ClientChatBoxPayload.ChatBoxDataToClient.CODEC, ClientChatBoxPayload.ChatBoxDataToClient::execute);
        registrar.playToClient(ClientChatBoxPayload.SetMaxTriggerCount.TYPE, ClientChatBoxPayload.SetMaxTriggerCount.CODEC, ClientChatBoxPayload.SetMaxTriggerCount::execute);
        registrar.playToClient(ClientChatBoxPayload.ResetMaxTriggerCount.TYPE, ClientChatBoxPayload.ResetMaxTriggerCount.CODEC, ClientChatBoxPayload.ResetMaxTriggerCount::execute);
        registrar.playToClient(ClientChatBoxPayload.SyncEntityData.TYPE, ClientChatBoxPayload.SyncEntityData.CODEC, ClientChatBoxPayload.SyncEntityData::execute);

        //c2s
        registrar.playToServer(ServerChatBoxPayload.SetMaxTriggerCount.TYPE, ServerChatBoxPayload.SetMaxTriggerCount.CODEC, ServerChatBoxPayload.SetMaxTriggerCount::execute);
        registrar.playToServer(ServerChatBoxPayload.ResetMaxTriggerCount.TYPE, ServerChatBoxPayload.ResetMaxTriggerCount.CODEC, ServerChatBoxPayload.ResetMaxTriggerCount::execute);
        registrar.playToServer(SendClickEvent.TYPE, SendClickEvent.CODEC, SendClickEvent::execute);

        registrar.playBidirectional(SimplePayload.TYPE, SimplePayload.CODEC, SimplePayload::execute);
    }
}
