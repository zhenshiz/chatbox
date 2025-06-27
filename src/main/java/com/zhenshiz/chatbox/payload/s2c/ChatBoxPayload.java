package com.zhenshiz.chatbox.payload.s2c;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

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

    public record OpenChatBox() implements CustomPacketPayload {
        public static final Type<OpenChatBox> TYPE = new Type<>(ChatBox.ResourceLocationMod("open_dialog"));
        public static final StreamCodec<FriendlyByteBuf, OpenChatBox> CODEC = StreamCodec.ofMember(OpenChatBox::write, OpenChatBox::new);

        public OpenChatBox(FriendlyByteBuf friendlyByteBuf) {
            this();
        }

        private void write(FriendlyByteBuf buf) {
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ToggleTheme(ResourceLocation theme) implements CustomPacketPayload {
        public static final Type<ToggleTheme> TYPE = new Type<>(ChatBox.ResourceLocationMod("toggle_theme"));
        public static final StreamCodec<FriendlyByteBuf, ToggleTheme> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                ToggleTheme::theme,
                ToggleTheme::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AllChatBoxThemeToClient(Map<ResourceLocation, String> themeMap) implements CustomPacketPayload {
        public static final Type<AllChatBoxThemeToClient> TYPE = new Type<>(ChatBox.ResourceLocationMod("all_chat_box_theme_to_client"));
        public static final StreamCodec<FriendlyByteBuf, AllChatBoxThemeToClient> CODEC = StreamCodec.composite(
                ByteBufCodecs.map(
                        HashMap::new,
                        ResourceLocation.STREAM_CODEC,
                        ByteBufCodecs.STRING_UTF8
                ),
                AllChatBoxThemeToClient::themeMap,
                AllChatBoxThemeToClient::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AllChatBoxDialoguesToClient(Map<ResourceLocation,String> dialoguesMap) implements CustomPacketPayload {
        public static final Type<AllChatBoxDialoguesToClient> TYPE = new Type<>(ChatBox.ResourceLocationMod("all_chat_box_dialogues_to_client"));
        public static final StreamCodec<FriendlyByteBuf, AllChatBoxDialoguesToClient> CODEC = StreamCodec.composite(
                ByteBufCodecs.map(
                        HashMap::new,
                        ResourceLocation.STREAM_CODEC,
                        ByteBufCodecs.STRING_UTF8
                ),
                AllChatBoxDialoguesToClient::dialoguesMap,
                AllChatBoxDialoguesToClient::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
