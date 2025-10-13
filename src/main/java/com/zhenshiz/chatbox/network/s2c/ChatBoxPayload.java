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
import java.util.function.Consumer;

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
            if (ChatBoxUtil.themeResourceLocation != null) {
                ResourceLocation theme = ResourceLocation.tryParse(ChatBoxUtil.themeResourceLocation);
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

    public record SimplePayload(String name, String value) implements CustomPacketPayload {
        public static final Type<SimplePayload> TYPE = new Type<>(ChatBox.ResourceLocationMod("simple_payload"));
        public static final StreamCodec<FriendlyByteBuf, SimplePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SimplePayload::name,
                ByteBufCodecs.STRING_UTF8, SimplePayload::value,
                SimplePayload::new
        );
        private static final Map<String, Consumer<String>> handlers = new HashMap<>();
        static {
            handlers.put("open_dialog", s -> ChatBoxCommandUtil.clientOpenChatBox());
            handlers.put("set_theme", ChatBoxCommandUtil::clientToggleTheme);
            handlers.put("next_dialogue", s -> ChatBoxCommandUtil.clientNextDialogue());
            handlers.put("auto_play", s -> ChatBoxCommandUtil.clientAutoPlay(Boolean.parseBoolean(s)));
            handlers.put("set_is_screen", s -> ChatBoxCommandUtil.clientSetIsScreen(Boolean.parseBoolean(s)));
        }

        public static void execute(SimplePayload payload, ClientPlayNetworking.Context context) {
            if (handlers.containsKey(payload.name())) handlers.get(payload.name()).accept(payload.value());
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
