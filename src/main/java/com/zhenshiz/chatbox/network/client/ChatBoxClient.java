package com.zhenshiz.chatbox.network.client;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.payload.s2c.ClientChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ChatBox.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ChatBoxClient {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ChatBox.MOD_ID);
        registrar.playBidirectional(
                ClientChatBoxPayload.OpenScreenPayload.TYPE,
                ClientChatBoxPayload.OpenScreenPayload.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxCommandUtil.clientSkipDialogues(payload.dialogues(), payload.group(), payload.index()),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ClientChatBoxPayload.OpenChatBox.TYPE,
                ClientChatBoxPayload.OpenChatBox.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxCommandUtil.clientOpenChatBox(),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ClientChatBoxPayload.ToggleTheme.TYPE,
                ClientChatBoxPayload.ToggleTheme.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxCommandUtil.clientToggleTheme(payload.theme()),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ClientChatBoxPayload.AllChatBoxThemeToClient.TYPE,
                ClientChatBoxPayload.AllChatBoxThemeToClient.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> {
                            ChatBoxUtil.setTheme(mergeString(payload.themeMap()));
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
                ClientChatBoxPayload.AllChatBoxDialoguesToClient.TYPE,
                ClientChatBoxPayload.AllChatBoxDialoguesToClient.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxUtil.setDialogues(mergeString(payload.dialoguesMap())),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ClientChatBoxPayload.SetMaxTriggerCount.TYPE,
                ClientChatBoxPayload.SetMaxTriggerCount.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxCommandUtil.clientSetMaxTriggerCount(payload.resourceLocation(), payload.maxTriggerCount()),
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ClientChatBoxPayload.SetMaxTriggerCountPlus.TYPE,
                ClientChatBoxPayload.SetMaxTriggerCountPlus.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> {
                            ChatBoxTriggerCount.MaxTriggerCount maxTriggerCount = payload.maxTriggerCount();
                            context.player().setData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT, maxTriggerCount);
                        },
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                ClientChatBoxPayload.ResetMaxTriggerCount.TYPE,
                ClientChatBoxPayload.ResetMaxTriggerCount.CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> ChatBoxCommandUtil.clientResetMaxTriggerCount(),
                        (payload, context) -> {
                        }
                )
        );
    }

    private static Map<ResourceLocation, String> mergeString(Map<ResourceLocation, List<String>> map) {
        Map<ResourceLocation, String> result = new HashMap<>();
        for (var entry : map.entrySet()) {
            ResourceLocation rl = entry.getKey();
            List<String> parts = entry.getValue();
            StringBuilder builder = new StringBuilder();
            for (String part : parts) {
                builder.append(part);
            }
            result.put(rl, builder.toString());
        }
        return result;
    }
}