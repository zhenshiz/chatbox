package com.zhenshiz.chatbox.payload.s2c;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ChatBoxPayload {
    public record OpenScreenPayload(ResourceLocation dialogues, String group,
                                    int index) implements CustomPacketPayload {
        public static final Type<OpenScreenPayload> TYPE = new Type<>(ChatBox.ResourceLocationMod("open_screen"));
        public static final StreamCodec<FriendlyByteBuf, OpenScreenPayload> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                OpenScreenPayload::dialogues,
                ByteBufCodecs.STRING_UTF8,
                OpenScreenPayload::group,
                ByteBufCodecs.INT,
                OpenScreenPayload::index,
                OpenScreenPayload::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record openChatBox() implements CustomPacketPayload {
        public static final Type<openChatBox> TYPE = new Type<>(ChatBox.ResourceLocationMod("open_dialog"));
        public static final StreamCodec<FriendlyByteBuf, openChatBox> CODEC = StreamCodec.ofMember(openChatBox::write, openChatBox::new);

        public openChatBox(FriendlyByteBuf friendlyByteBuf) {
            this();
        }

        private void write(FriendlyByteBuf buf) {
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
