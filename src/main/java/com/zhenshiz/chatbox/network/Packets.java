package com.zhenshiz.chatbox.network;

//? fabric {
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
    //? >= 1.21 {
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
    //?}

public class Packets {

    public static void register() {
        //? >= 1.21 {
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.ChatBoxDataToClient.TYPE, ChatBoxPayload.ChatBoxDataToClient.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.SyncEntityData.TYPE, ChatBoxPayload.SyncEntityData.CODEC);
        PayloadTypeRegistry.playS2C().register(SimplePayload.TYPE, SimplePayload.CODEC);

        PayloadTypeRegistry.playC2S().register(SimplePayload.TYPE, SimplePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SendClickEvent.TYPE, SendClickEvent.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SimplePayload.TYPE, SimplePayload::execute);
        ServerPlayNetworking.registerGlobalReceiver(SendClickEvent.TYPE, SendClickEvent::execute);
        //?} else {
        /*ServerPlayNetworking.registerGlobalReceiver(SendClickEvent.ID, (server, player, h, buf, r) -> SendClickEvent.handleOnServer(player, SendClickEvent.decode(buf)));
        ServerPlayNetworking.registerGlobalReceiver(SimplePayload.ID, (server, player, h, buf, r) -> SimplePayload.handleOnServer(player, SimplePayload.decode(buf)));
        *///?}
    }
}//?}

//? neoforge {
//?}

//? forge {
/*import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Packets {
    private static final Supplier<String> VERSION = () -> "chatbox";
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
            DATA_TO_CLIENT = registerChannel(ChatBoxPayload.ChatBoxDataToClient.class),
            SYNC_ENTITY_DATA = registerChannel(ChatBoxPayload.SyncEntityData.class),
            SIMPLE_PAYLOAD = registerChannel(SimplePayload.class),

    SEND_CLICK_EVENT = registerChannel(SendClickEvent.class);

    private static int id = 0;
    private static int nextId() {return ++id;}

    public static void register() {
        DATA_TO_CLIENT.registerMessage(nextId(), ChatBoxPayload.ChatBoxDataToClient.class, ChatBoxPayload.ChatBoxDataToClient::encode, ChatBoxPayload.ChatBoxDataToClient::decode, (packet, ctx) -> {
            ctx.get().enqueueWork(() -> ChatBoxPayload.ChatBoxDataToClient.handleOnClient(packet));
            ctx.get().setPacketHandled(true);
        });

        SYNC_ENTITY_DATA.registerMessage(nextId(), ChatBoxPayload.SyncEntityData.class, ChatBoxPayload.SyncEntityData::encode, ChatBoxPayload.SyncEntityData::decode, (packet, ctx) -> {
            ctx.get().enqueueWork(() -> ChatBoxPayload.SyncEntityData.handleOnClient(packet));
            ctx.get().setPacketHandled(true);
        });

        SIMPLE_PAYLOAD.registerMessage(nextId(), SimplePayload.class, SimplePayload::encode, SimplePayload::decode, (packet, ctx) -> {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender != null) SimplePayload.handleOnServer(sender, packet);
                else SimplePayload.handleOnClient(packet);
            });
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
*///?}
