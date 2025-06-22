package com.zhenshiz.chatbox.network.server;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.payload.c2s.SendCommandPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ChatBox.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class SendCommand {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ChatBox.MOD_ID);
        registrar.playBidirectional(
                SendCommandPayload.TYPE,
                SendCommandPayload.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> {
                        },
                        (payload, context) -> {
                            ServerPlayer player = (ServerPlayer) context.player();
                            String command = payload.command();

                            CommandSourceStack commandSource = player.createCommandSourceStack()
                                    .withPermission(Commands.LEVEL_GAMEMASTERS)
                                    .withSuppressedOutput();

                            try {
                                player.server.getCommands().performPrefixedCommand(commandSource, command);
                            } catch (Exception e) {
                                ChatBox.LOGGER.error("Error executing command on server: {}", command, e);
                            }
                        }
                )
        );
    }
}
