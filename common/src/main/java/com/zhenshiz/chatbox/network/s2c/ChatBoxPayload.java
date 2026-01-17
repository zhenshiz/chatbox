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
    public record ChatBoxDataToClient(String name, Map<Identifier, List<String>> dataMap) implements CustomPacketPayload {
        public static final Type<ChatBoxDataToClient> TYPE = new Type<>(ChatBox.id("chat_box_data_to_client"));
        public static final StreamCodec<FriendlyByteBuf, ChatBoxDataToClient> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ChatBoxDataToClient::name,
                ByteBufCodecs.map(
                        HashMap::new,
                        Identifier.STREAM_CODEC,
                        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                ), ChatBoxDataToClient::dataMap,
                ChatBoxDataToClient::new
        );

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handleOnClient(ChatBoxDataToClient payload) {
            switch (payload.name()) {
                case "theme" -> {
                    ChatBoxUtil.setTheme(mergeString(payload.dataMap()));
                    if (ChatBoxUtil.themeIdentifier != null) {
                        ChatBoxCommandUtil.clientToggleTheme(ChatBoxUtil.themeIdentifier);
                    }
                }
                case "dialogues" -> ChatBoxUtil.setDialogues(mergeString(payload.dataMap()));
            }
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
