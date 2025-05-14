package com.zhenshiz.chatbox.network.client;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.DialogBox;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ChatBox.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ChatBoxClient {
    private static final Minecraft minecraft = Minecraft.getInstance();

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ChatBox.MOD_ID);
        registrar.playBidirectional(
                ChatBoxPayload.OpenScreenPayload.TYPE,
                ChatBoxPayload.OpenScreenPayload.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxUtil.skipDialogues(context.player().getUUID(), payload.dialogues(), payload.group(), payload.index()),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ChatBoxPayload.openChatBox.TYPE,
                ChatBoxPayload.openChatBox.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> {
                            if (minecraft.player != null) {
                                DialogBox dialogBox = ChatBoxUtil.chatBoxScreens.get(minecraft.player.getUUID()).dialogBox;
                                ResourceLocation dialoguesResourceLocation = dialogBox.dialoguesResourceLocation;
                                String group = dialogBox.group;
                                Integer index = dialogBox.index;
                                if (dialoguesResourceLocation != null && group != null && index != null) {
                                    ChatBoxUtil.skipDialogues(minecraft.player.getUUID(), dialoguesResourceLocation, group, index);
                                }
                            }
                        },
                        (payload, context) -> {
                        }
                )
        );
    }
}