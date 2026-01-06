package com.zhenshiz.chatbox.network.s2c;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ClientChatBoxPayload {

    public record ChatBoxDataToClient(String name, Map<ResourceLocation, List<String>> dataMap) implements CustomPacketPayload {
        public static final Type<ChatBoxDataToClient> TYPE = new Type<>(ChatBox.id("chat_box_data_to_client"));
        public static final StreamCodec<FriendlyByteBuf, ChatBoxDataToClient> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ChatBoxDataToClient::name,
                ByteBufCodecs.map(
                        HashMap::new,
                        ResourceLocation.STREAM_CODEC,
                        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                ), ChatBoxDataToClient::dataMap,
                ChatBoxDataToClient::new
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {return TYPE;}

        public static void execute(ChatBoxDataToClient payload, IPayloadContext context) {
            switch (payload.name()) {
                case "theme" -> {
                    ChatBoxUtil.setTheme(mergeString(payload.dataMap()));
                    if (ChatBoxUtil.themeResourceLocation != null) {
                        ChatBoxCommandUtil.clientToggleTheme(ChatBoxUtil.themeResourceLocation);
                    }
                }
                case "dialogues" -> ChatBoxUtil.setDialogues(mergeString(payload.dataMap()));
            }
        }
    }

    public record SetMaxTriggerCount(ResourceLocation resourceLocation,
                                     int maxTriggerCount) implements CustomPacketPayload {
        public static final Type<SetMaxTriggerCount> TYPE = new Type<>(ChatBox.id("client_set_max_trigger_count"));
        public static final StreamCodec<FriendlyByteBuf, SetMaxTriggerCount> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                SetMaxTriggerCount::resourceLocation,
                ByteBufCodecs.INT,
                SetMaxTriggerCount::maxTriggerCount,
                SetMaxTriggerCount::new
        );

        public static void execute(SetMaxTriggerCount payload, IPayloadContext context) {
            ChatBoxCommandUtil.clientSetMaxTriggerCount(payload.resourceLocation(), payload.maxTriggerCount());
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ResetMaxTriggerCount() implements CustomPacketPayload {
        public static final Type<ResetMaxTriggerCount> TYPE = new Type<>(ChatBox.id("client_reset_max_trigger_count"));
        public static final StreamCodec<FriendlyByteBuf, ResetMaxTriggerCount> CODEC = StreamCodec.ofMember(ResetMaxTriggerCount::write, ResetMaxTriggerCount::new);

        public ResetMaxTriggerCount(FriendlyByteBuf friendlyByteBuf) {
            this();
        }

        private void write(FriendlyByteBuf buf) {
        }

        public static void execute(ResetMaxTriggerCount payload, IPayloadContext context) {
            ChatBoxCommandUtil.clientResetMaxTriggerCount();
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
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

        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void execute(SyncEntityData payload, IPayloadContext context) {
            ChatBoxUtil.setChatTargets(payload.entities());
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
