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
                        (payload, context) -> ChatBoxUtil.skipDialogues(payload.dialogues(), payload.group(), payload.index()),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ChatBoxPayload.OpenChatBox.TYPE,
                ChatBoxPayload.OpenChatBox.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> {
                            if (minecraft.player != null) {
                                DialogBox dialogBox = ChatBoxUtil.chatBoxScreen.dialogBox;
                                ResourceLocation dialoguesResourceLocation = dialogBox.dialoguesResourceLocation;
                                String group = dialogBox.group;
                                Integer index = dialogBox.index;
                                if (dialoguesResourceLocation != null && group != null && index != null) {
                                    ChatBoxUtil.skipDialogues(dialoguesResourceLocation, group, index);
                                }
                            }
                        },
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ChatBoxPayload.ToggleTheme.TYPE,
                ChatBoxPayload.ToggleTheme.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxUtil.toggleTheme(payload.theme()),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ChatBoxPayload.AllChatBoxThemeToClient.TYPE,
                ChatBoxPayload.AllChatBoxThemeToClient.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxUtil.setTheme(payload.themeMap()),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ChatBoxPayload.AllChatBoxDialoguesToClient.TYPE,
                ChatBoxPayload.AllChatBoxDialoguesToClient.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxUtil.setDialogues(payload.dialoguesMap()),
                        (payload, context) -> {
                        }
                )
        );
    }
}