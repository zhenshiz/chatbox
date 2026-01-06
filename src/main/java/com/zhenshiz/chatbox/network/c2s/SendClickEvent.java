package com.zhenshiz.chatbox.network.c2s;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.api.EventExecutor;
import net.minecraft.network.FriendlyByteBuf;
//? >= 1.21 {
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
//?} else {
/*import com.zhenshiz.chatbox.network.CustomPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
*///?}

//? >= 1.21 {
public record SendClickEvent(String typeId, String value) implements CustomPacketPayload {
    public static final Type<SendClickEvent> TYPE = new Type<>(ChatBox.id("execute_click_event"));
    public static final StreamCodec<FriendlyByteBuf, SendClickEvent> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SendClickEvent::typeId,
            ByteBufCodecs.STRING_UTF8, SendClickEvent::value,
            SendClickEvent::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {return TYPE;}

    public static void execute(SendClickEvent payload, ServerPlayNetworking.Context context) {
        EventExecutor.EXECUTORS.get(payload.typeId()).executeOnServer(context.player(), payload.value());
    }
}//?} else {
/*public record SendClickEvent(String name, String value) implements CustomPacket {
    public ResourceLocation id() {return ID;}
    public static final ResourceLocation ID = ChatBox.id("execute_click_event");

    public void write(FriendlyByteBuf buf) {encode(this, buf);}

    public static void encode(SendClickEvent packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.name);
        buf.writeUtf(packet.value);
    }

    public static SendClickEvent decode(FriendlyByteBuf buf) {
        return new SendClickEvent(buf.readUtf(), buf.readUtf());
    }

    public static void handleOnServer(ServerPlayer player, SendClickEvent packet) {
        EventExecutor.EXECUTORS.get(packet.name()).executeOnServer(player, packet.value());
    }
}*///?}
