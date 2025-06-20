package com.zhenshiz.chatbox.network.client;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ChatBox.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ChatBoxClient {

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
                        (payload, context) -> ChatBoxCommandUtil.clientOpenChatBox(),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ChatBoxPayload.ToggleTheme.TYPE,
                ChatBoxPayload.ToggleTheme.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxCommandUtil.clientToggleTheme(payload.theme()),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ChatBoxPayload.AllChatBoxThemeToClient.TYPE,
                ChatBoxPayload.AllChatBoxThemeToClient.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> {
                            ChatBoxUtil.setTheme(payload.themeMap());
                            if (ChatBoxCommandUtil.themeResourceLocation != null) {
                                ResourceLocation theme = ResourceLocation.tryParse(ChatBoxCommandUtil.themeResourceLocation);
                                if (theme != null) {
                                    ChatBoxUtil.toggleTheme(theme);
                                }
                            }
                        },
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