package com.zhenshiz.chatbox.network.c2s;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.api.ChatOptionClickEvent;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record SendClickEvent(String typeId, String value) implements CustomPacketPayload {
    public static final Type<SendClickEvent> TYPE = new Type<>(ChatBox.ResourceLocationMod("execute_click_event"));
    public static final StreamCodec<FriendlyByteBuf, SendClickEvent> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SendClickEvent::typeId,
            ByteBufCodecs.STRING_UTF8, SendClickEvent::getParsedValue,
            SendClickEvent::new
    );

    private String getParsedValue() {
        return ChatBoxUtil.parseTargetPlaceholders(value);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {return TYPE;}

    public static void execute(SendClickEvent payload, ServerPlayNetworking.Context context) {
        ChatOptionClickEvent.CLICK_EVENTS.get(payload.typeId()).executeOnServer(context.player(), payload.value());
    }
}
