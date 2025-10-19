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
        registrar.playToClient(ClientChatBoxPayload.OpenScreenPayload.TYPE, ClientChatBoxPayload.OpenScreenPayload.CODEC, ClientChatBoxPayload.OpenScreenPayload::execute);
        registrar.playToClient(ClientChatBoxPayload.AllChatBoxThemeToClient.TYPE, ClientChatBoxPayload.AllChatBoxThemeToClient.CODEC, ClientChatBoxPayload.AllChatBoxThemeToClient::execute);
        registrar.playToClient(ClientChatBoxPayload.AllChatBoxDialoguesToClient.TYPE, ClientChatBoxPayload.AllChatBoxDialoguesToClient.CODEC, ClientChatBoxPayload.AllChatBoxDialoguesToClient::execute);
        registrar.playToClient(ClientChatBoxPayload.SetMaxTriggerCount.TYPE, ClientChatBoxPayload.SetMaxTriggerCount.CODEC, ClientChatBoxPayload.SetMaxTriggerCount::execute);
        registrar.playToClient(ClientChatBoxPayload.SetMaxTriggerCountPlus.TYPE, ClientChatBoxPayload.SetMaxTriggerCountPlus.CODEC, ClientChatBoxPayload.SetMaxTriggerCountPlus::execute);
        registrar.playToClient(ClientChatBoxPayload.ResetMaxTriggerCount.TYPE, ClientChatBoxPayload.ResetMaxTriggerCount.CODEC, ClientChatBoxPayload.ResetMaxTriggerCount::execute);
        registrar.playToClient(ClientChatBoxPayload.SimplePayload.TYPE, ClientChatBoxPayload.SimplePayload.CODEC, ClientChatBoxPayload.SimplePayload::execute);

        //c2s
        registrar.playToServer(ServerChatBoxPayload.SetMaxTriggerCountPayload.TYPE, ServerChatBoxPayload.SetMaxTriggerCountPayload.CODEC, ServerChatBoxPayload.SetMaxTriggerCountPayload::execute);
        registrar.playToServer(ServerChatBoxPayload.ResetMaxTriggerCount.TYPE, ServerChatBoxPayload.ResetMaxTriggerCount.CODEC, ServerChatBoxPayload.ResetMaxTriggerCount::execute);
        registrar.playToServer(SendClickEvent.TYPE, SendClickEvent.CODEC, SendClickEvent::execute);
    }
}
