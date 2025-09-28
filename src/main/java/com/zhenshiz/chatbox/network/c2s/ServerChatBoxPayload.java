package com.zhenshiz.chatbox.network.c2s;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ServerChatBoxPayload {
    public record SetMaxTriggerCountPayload(ResourceLocation resourceLocation,
                                            int maxTriggerCount) implements CustomPacketPayload {
        public static final Type<SetMaxTriggerCountPayload> TYPE = new Type<>(ChatBox.ResourceLocationMod("server_set_max_trigger_count"));
        public static final StreamCodec<FriendlyByteBuf, SetMaxTriggerCountPayload> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                SetMaxTriggerCountPayload::resourceLocation,
                ByteBufCodecs.INT,
                SetMaxTriggerCountPayload::maxTriggerCount,
                SetMaxTriggerCountPayload::new
        );

        public static void execute(SetMaxTriggerCountPayload payload, IPayloadContext context) {
            ServerPlayer player = (ServerPlayer) context.player();
            ChatBoxCommandUtil.serverSetMaxTriggerCount(player, payload.resourceLocation(), payload.maxTriggerCount());
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ResetMaxTriggerCount() implements CustomPacketPayload {
        public static final Type<ResetMaxTriggerCount> TYPE = new Type<>(ChatBox.ResourceLocationMod("server_reset_max_trigger_count"));
        public static final StreamCodec<FriendlyByteBuf, ResetMaxTriggerCount> CODEC = StreamCodec.ofMember(ResetMaxTriggerCount::write, ResetMaxTriggerCount::new);

        public ResetMaxTriggerCount(FriendlyByteBuf friendlyByteBuf) {
            this();
        }

        private void write(FriendlyByteBuf buf) {
        }

        public static void execute(ResetMaxTriggerCount payload, IPayloadContext context) {
            ServerPlayer player = (ServerPlayer) context.player();
            ChatBoxCommandUtil.serverResetMaxTriggerCount(player);
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
