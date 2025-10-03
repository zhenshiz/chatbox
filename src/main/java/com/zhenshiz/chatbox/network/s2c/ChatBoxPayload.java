package com.zhenshiz.chatbox.network.s2c;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        public static void execute(OpenScreenPayload payload, ClientPlayNetworking.Context context) {
            ChatBoxCommandUtil.clientSkipDialogues(payload.dialogues(), payload.group(), payload.index());
        }
    }

    public record OpenChatBox() implements CustomPacketPayload {
        public static final Type<OpenChatBox> TYPE = new Type<>(ChatBox.ResourceLocationMod("open_dialog"));
        public static final StreamCodec<FriendlyByteBuf, OpenChatBox> CODEC = StreamCodec.ofMember(OpenChatBox::write, OpenChatBox::new);

        public OpenChatBox(FriendlyByteBuf friendlyByteBuf) {this();}

        private void write(FriendlyByteBuf buf) {}

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void execute(OpenChatBox payload, ClientPlayNetworking.Context context) {
            ChatBoxCommandUtil.clientOpenChatBox();
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

        public static void execute(ToggleTheme payload, ClientPlayNetworking.Context context) {
            ChatBoxCommandUtil.clientToggleTheme(payload.theme());
        }
    }

    public record AllChatBoxThemeToClient(Map<ResourceLocation, List<String>> themeMap) implements CustomPacketPayload {
        public static final Type<AllChatBoxThemeToClient> TYPE = new Type<>(ChatBox.ResourceLocationMod("all_chat_box_theme_to_client"));
        public static final StreamCodec<FriendlyByteBuf, AllChatBoxThemeToClient> CODEC = StreamCodec.composite(
                ByteBufCodecs.map(
                        HashMap::new,
                        ResourceLocation.STREAM_CODEC,
                        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                ),
                AllChatBoxThemeToClient::themeMap,
                AllChatBoxThemeToClient::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void execute(AllChatBoxThemeToClient payload, ClientPlayNetworking.Context context) {
            ChatBoxUtil.setTheme(mergeString(payload.themeMap()));
            if (ChatBoxCommandUtil.themeResourceLocation != null) {
                ResourceLocation theme = ResourceLocation.tryParse(ChatBoxCommandUtil.themeResourceLocation);
                if (theme != null) {
                    ChatBoxUtil.toggleTheme(theme);
                }
            }
        }
    }

    public record AllChatBoxDialoguesToClient(Map<ResourceLocation, List<String>> dialoguesMap) implements CustomPacketPayload {
        public static final Type<AllChatBoxDialoguesToClient> TYPE = new Type<>(ChatBox.ResourceLocationMod("all_chat_box_dialogues_to_client"));
        public static final StreamCodec<FriendlyByteBuf, AllChatBoxDialoguesToClient> CODEC = StreamCodec.composite(
                ByteBufCodecs.map(
                        HashMap::new,
                        ResourceLocation.STREAM_CODEC,
                        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                ),
                AllChatBoxDialoguesToClient::dialoguesMap,
                AllChatBoxDialoguesToClient::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void execute(AllChatBoxDialoguesToClient payload, ClientPlayNetworking.Context context) {
            ChatBoxUtil.setDialogues(mergeString(payload.dialoguesMap()));
        }
    }

    public record NextDialoguePayload() implements CustomPacketPayload {
        public static final Type<NextDialoguePayload> TYPE = new Type<>(ChatBox.ResourceLocationMod("client_next_dialogue"));
        public static final StreamCodec<FriendlyByteBuf, NextDialoguePayload> CODEC = StreamCodec.ofMember(NextDialoguePayload::write, NextDialoguePayload::new);

        public NextDialoguePayload(FriendlyByteBuf friendlyByteBuf) {this();}

        private void write(FriendlyByteBuf buf) {}

        public static void execute(NextDialoguePayload payload, ClientPlayNetworking.Context context) {
            ChatBoxCommandUtil.clientNextDialogue();
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AutoPlayPayload(boolean autoPlay) implements CustomPacketPayload {
        public static final Type<AutoPlayPayload> TYPE = new Type<>(ChatBox.ResourceLocationMod("client_auto_play"));
        public static final StreamCodec<FriendlyByteBuf, AutoPlayPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                AutoPlayPayload::autoPlay,
                AutoPlayPayload::new
        );

        public static void execute(AutoPlayPayload payload, ClientPlayNetworking.Context context) {
            ChatBoxCommandUtil.clientAutoPlay(payload.autoPlay());
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static Map<ResourceLocation, String> mergeString(Map<ResourceLocation, List<String>> map) {
        Map<ResourceLocation, String> result = new HashMap<>();
        for (var entry : map.entrySet()) {
            ResourceLocation rl = entry.getKey();
            List<String> parts = entry.getValue();
            StringBuilder builder = new StringBuilder();
            for (String part : parts) {
                builder.append(part);
            }
            result.put(rl, builder.toString());
        }
        return result;
    }
}
