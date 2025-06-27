package com.zhenshiz.chatbox.payload.s2c;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ClientChatBoxPayload {
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

    public record AllChatBoxDialoguesToClient(
            Map<ResourceLocation, String> dialoguesMap) implements CustomPacketPayload {
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

    public record SetMaxTriggerCount(ResourceLocation resourceLocation,
                                     int maxTriggerCount) implements CustomPacketPayload {
        public static final Type<SetMaxTriggerCount> TYPE = new Type<>(ChatBox.ResourceLocationMod("client_set_max_trigger_count"));
        public static final StreamCodec<FriendlyByteBuf, SetMaxTriggerCount> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                SetMaxTriggerCount::resourceLocation,
                ByteBufCodecs.INT,
                SetMaxTriggerCount::maxTriggerCount,
                SetMaxTriggerCount::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetMaxTriggerCountPlus(
            ChatBoxTriggerCount.MaxTriggerCount maxTriggerCount) implements CustomPacketPayload {
        public static final Type<SetMaxTriggerCountPlus> TYPE = new Type<>(ChatBox.ResourceLocationMod("client_set_max_trigger_count_plus"));
        public static final StreamCodec<FriendlyByteBuf, SetMaxTriggerCountPlus> CODEC = StreamCodec.composite(
                ChatBoxTriggerCount.MaxTriggerCount.STREAM_CODEC,
                SetMaxTriggerCountPlus::maxTriggerCount,
                SetMaxTriggerCountPlus::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ResetMaxTriggerCount() implements CustomPacketPayload {
        public static final Type<ResetMaxTriggerCount> TYPE = new Type<>(ChatBox.ResourceLocationMod("client_reset_max_trigger_count"));
        public static final StreamCodec<FriendlyByteBuf, ResetMaxTriggerCount> CODEC = StreamCodec.ofMember(ResetMaxTriggerCount::write, ResetMaxTriggerCount::new);

        public ResetMaxTriggerCount(FriendlyByteBuf friendlyByteBuf) {
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
