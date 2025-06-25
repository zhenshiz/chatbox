package com.zhenshiz.chatbox.payload.c2s;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record SendCommandPayload(String command) implements CustomPacketPayload {
    public static final Type<SendCommandPayload> TYPE = new Type<>(ChatBox.ResourceLocationMod("execute_server_command"));
    public static final StreamCodec<FriendlyByteBuf, SendCommandPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SendCommandPayload::command,
            SendCommandPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
