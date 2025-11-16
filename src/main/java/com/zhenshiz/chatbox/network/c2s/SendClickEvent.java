package com.zhenshiz.chatbox.network.c2s;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.api.EventExecutor;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SendClickEvent(String clickType, String value) implements CustomPacketPayload {
    public static final Type<SendClickEvent> TYPE = new Type<>(ChatBox.ResourceLocationMod("execute_click_event"));
    public static final StreamCodec<FriendlyByteBuf, SendClickEvent> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SendClickEvent::clickType,
            ByteBufCodecs.STRING_UTF8,
            SendClickEvent::getParsedValue,
            SendClickEvent::new
    );

    private String getParsedValue() {
        return ChatBoxUtil.parseTargetPlaceholders(value);
    }

    public static void execute(SendClickEvent payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        EventExecutor.EXECUTORS.get(payload.clickType()).executeOnServer(player, payload.value());
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
