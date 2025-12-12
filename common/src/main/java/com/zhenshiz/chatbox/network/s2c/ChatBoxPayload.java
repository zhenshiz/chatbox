package com.zhenshiz.chatbox.network.s2c;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("all")
public class ChatBoxPayload {
    public record OpenScreen(Identifier dialogues, String group, int index) implements CustomPacketPayload {
        public static final Type<OpenScreen> TYPE = new Type<>(ChatBox.id("open_screen"));
        public static final StreamCodec<FriendlyByteBuf, OpenScreen> CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC,  OpenScreen::dialogues,
                ByteBufCodecs.STRING_UTF8,      OpenScreen::group,
                ByteBufCodecs.INT,              OpenScreen::index,
                OpenScreen::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handleOnClient(OpenScreen payload) {
            ChatBoxCommandUtil.clientSkipDialogues(payload.dialogues(), payload.group(), payload.index());
        }
    }

    public record AllChatBoxThemeToClient(Map<Identifier, List<String>> themeMap) implements CustomPacketPayload {
        public static final Type<AllChatBoxThemeToClient> TYPE = new Type<>(ChatBox.id("all_chat_box_theme_to_client"));
        public static final StreamCodec<FriendlyByteBuf, AllChatBoxThemeToClient> CODEC = StreamCodec.composite(
                ByteBufCodecs.map(
                        HashMap::new,
                        Identifier.STREAM_CODEC,
                        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                ),
                AllChatBoxThemeToClient::themeMap,
                AllChatBoxThemeToClient::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handleOnClient(AllChatBoxThemeToClient payload) {
            ChatBoxUtil.setTheme(mergeString(payload.themeMap()));
            if (ChatBoxUtil.themeIdentifier != null) {
                Identifier theme = Identifier.tryParse(ChatBoxUtil.themeIdentifier);
                if (theme != null) {
                    ChatBoxUtil.toggleTheme(theme);
                }
            }
        }
    }

    public record AllChatBoxDialoguesToClient(Map<Identifier, List<String>> dialoguesMap) implements CustomPacketPayload {
        public static final Type<AllChatBoxDialoguesToClient> TYPE = new Type<>(ChatBox.id("all_chat_box_dialogues_to_client"));
        public static final StreamCodec<FriendlyByteBuf, AllChatBoxDialoguesToClient> CODEC = StreamCodec.composite(
                ByteBufCodecs.map(
                        HashMap::new,
                        Identifier.STREAM_CODEC,
                        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                ),
                AllChatBoxDialoguesToClient::dialoguesMap,
                AllChatBoxDialoguesToClient::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handleOnClient(AllChatBoxDialoguesToClient payload) {
            ChatBoxUtil.setDialogues(mergeString(payload.dialoguesMap()));
        }
    }

    public record SyncEntityData(LinkedHashMap<Integer, CompoundTag> entities) implements CustomPacketPayload {
        public static final Type<SyncEntityData> TYPE = new Type<>(ChatBox.id("sync_entity_data"));
        public static final StreamCodec<FriendlyByteBuf, SyncEntityData> CODEC = StreamCodec.composite(
                ByteBufCodecs.map(
                        LinkedHashMap::new,
                        ByteBufCodecs.INT,
                        ByteBufCodecs.COMPOUND_TAG
                ),
                SyncEntityData::entities,
                SyncEntityData::new
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handleOnClient(SyncEntityData payload) {
            ChatBoxUtil.setChatTargets(payload.entities());
        }
    }

    private static Map<Identifier, String> mergeString(Map<Identifier, List<String>> map) {
        Map<Identifier, String> result = new HashMap<>();
        for (var entry : map.entrySet()) {
            Identifier rl = entry.getKey();
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
