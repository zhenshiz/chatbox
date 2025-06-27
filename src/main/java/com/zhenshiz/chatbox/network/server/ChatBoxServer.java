package com.zhenshiz.chatbox.network.server;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.payload.c2s.ServerChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ChatBox.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ChatBoxServer {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ChatBox.MOD_ID);
        registrar.playBidirectional(
                ServerChatBoxPayload.SetMaxTriggerCountPayload.TYPE,
                ServerChatBoxPayload.SetMaxTriggerCountPayload.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> {
                        },
                        (payload, context) -> {
                            ServerPlayer player = (ServerPlayer) context.player();
                            ChatBoxCommandUtil.serverSetMaxTriggerCount(player, payload.resourceLocation(), payload.maxTriggerCount());
                        }
                )
        );

        registrar.playBidirectional(
                ServerChatBoxPayload.ResetMaxTriggerCount.TYPE,
                ServerChatBoxPayload.ResetMaxTriggerCount.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> {
                        },
                        (payload, context) -> {
                            ServerPlayer player = (ServerPlayer) context.player();
                            ChatBoxCommandUtil.serverResetMaxTriggerCount(player);
                        }
                )
        );
    }
}
