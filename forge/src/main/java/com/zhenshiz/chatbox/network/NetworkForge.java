package com.zhenshiz.chatbox.network;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class NetworkForge {
    private static final Supplier<String> VERSION = () -> ChatBox.MOD_ID;
    private static final Predicate<String> ACCEPT = version -> version.equals(VERSION.get());
    private static final Map<Class<? extends CustomPacket>, SimpleChannel> CHANNELS = new HashMap<>();

    private static SimpleChannel registerChannel(Class<? extends CustomPacket> packet) {
        try {
            SimpleChannel channel = NetworkRegistry.newSimpleChannel((ResourceLocation) packet.getField("ID").get(null), VERSION, ACCEPT, ACCEPT);
            CHANNELS.put(packet, channel);
            return channel;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static final SimpleChannel
            OPEN_SCREEN = registerChannel(ChatBoxPayload.OpenScreen.class),
            ALL_CHATBOX_THEME_TO_CLIENT = registerChannel(ChatBoxPayload.AllChatBoxThemeToClient.class),
            ALL_CHATBOX_DIALOGUES_TO_CLIENT = registerChannel(ChatBoxPayload.AllChatBoxDialoguesToClient.class),
            SIMPLE_PAYLOAD = registerChannel(ChatBoxPayload.SimplePayload.class),

    SEND_CLICK_EVENT = registerChannel(SendClickEvent.class);

    private static int id = 0;
    private static int nextId() {return ++id;}

    public static void registerHandlers() {

        OPEN_SCREEN.registerMessage(nextId(), ChatBoxPayload.OpenScreen.class, ChatBoxPayload.OpenScreen::encode, ChatBoxPayload.OpenScreen::decode, (packet, ctx) -> {
            ctx.get().enqueueWork(() -> ChatBoxPayload.OpenScreen.handleOnClient(packet));
            ctx.get().setPacketHandled(true);
        });

        ALL_CHATBOX_THEME_TO_CLIENT.registerMessage(nextId(), ChatBoxPayload.AllChatBoxThemeToClient.class, ChatBoxPayload.AllChatBoxThemeToClient::encode, ChatBoxPayload.AllChatBoxThemeToClient::decode, (packet, ctx) -> {
            ctx.get().enqueueWork(() -> ChatBoxPayload.AllChatBoxThemeToClient.handleOnClient(packet));
            ctx.get().setPacketHandled(true);
        });

        ALL_CHATBOX_DIALOGUES_TO_CLIENT.registerMessage(nextId(), ChatBoxPayload.AllChatBoxDialoguesToClient.class, ChatBoxPayload.AllChatBoxDialoguesToClient::encode, ChatBoxPayload.AllChatBoxDialoguesToClient::decode, (packet, ctx) -> {
            ctx.get().enqueueWork(() -> ChatBoxPayload.AllChatBoxDialoguesToClient.handleOnClient(packet));
            ctx.get().setPacketHandled(true);
        });

        SIMPLE_PAYLOAD.registerMessage(nextId(), ChatBoxPayload.SimplePayload.class, ChatBoxPayload.SimplePayload::encode, ChatBoxPayload.SimplePayload::decode, (packet, ctx) -> {
            ctx.get().enqueueWork(() -> ChatBoxPayload.SimplePayload.handleOnClient(packet));
            ctx.get().setPacketHandled(true);
        });

        SEND_CLICK_EVENT.registerMessage(nextId(), SendClickEvent.class, SendClickEvent::encode, SendClickEvent::decode, (packet, ctx) -> {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender != null) SendClickEvent.handleOnServer(sender, packet);
            });
            ctx.get().setPacketHandled(true);
        });
    }

    public static SimpleChannel getChannel(CustomPacket packet) {
        return CHANNELS.get(packet.getClass());
    }
}
