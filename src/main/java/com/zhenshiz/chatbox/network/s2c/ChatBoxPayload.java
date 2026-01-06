package com.zhenshiz.chatbox.network.s2c;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? < 1.21 {
/*import com.google.common.collect.Maps;
import com.zhenshiz.chatbox.network.CustomPacket;
*///?} else {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
//?}

import java.util.*;

public class ChatBoxPayload {

    //? >= 1.21 {
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

        public static void execute(ChatBoxDataToClient payload, ClientPlayNetworking.Context context) {
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

        public static void execute(SyncEntityData payload, ClientPlayNetworking.Context context) {
            ChatBoxUtil.setChatTargets(payload.entities());
        }
    }
    //?} else {
    /*public record ChatBoxDataToClient(String name, Map<ResourceLocation, List<String>> dataMap) implements CustomPacket {
        public ResourceLocation id() {return ID;}
        public static final ResourceLocation ID = ChatBox.id("chatbox_data_to_client");

        public void write(FriendlyByteBuf buf) {encode(this, buf);}

        public static void encode(ChatBoxDataToClient packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.name);
            buf.writeMap(packet.dataMap, FriendlyByteBuf::writeResourceLocation, (vBuf, v) -> vBuf.writeCollection(v, FriendlyByteBuf::writeUtf));
        }

        public static ChatBoxDataToClient decode(FriendlyByteBuf buf) {
            return new ChatBoxDataToClient(buf.readUtf(), buf.readMap(FriendlyByteBuf::readResourceLocation, v -> v.readList(FriendlyByteBuf::readUtf)));
        }

        public static void handleOnClient(ChatBoxDataToClient packet) {
            ChatBox.PLATFORM.runOnClient(() -> {
                switch (packet.name()) {
                    case "theme" -> {
                        ChatBoxUtil.setTheme(mergeString(packet.dataMap()));
                        if (ChatBoxUtil.themeResourceLocation != null) {
                            ChatBoxCommandUtil.clientToggleTheme(ChatBoxUtil.themeResourceLocation);
                        }
                    }
                    case "dialogues" -> ChatBoxUtil.setDialogues(mergeString(packet.dataMap()));
                }
            });
        }
    }

    public record SyncEntityData(LinkedHashMap<Integer, CompoundTag> entities) implements CustomPacket {
        public ResourceLocation id() {return ID;}
        public static final ResourceLocation ID = ChatBox.id("sync_entity_data");

        public void write(FriendlyByteBuf buf) {encode(this, buf);}

        public static void encode(SyncEntityData packet, FriendlyByteBuf buf) {
            buf.writeMap(packet.entities, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeNbt);
        }

        public static SyncEntityData decode(FriendlyByteBuf buf) {
            return new SyncEntityData(buf.readMap(Maps::newLinkedHashMapWithExpectedSize, FriendlyByteBuf::readInt, FriendlyByteBuf::readNbt));
        }

        public static void handleOnClient(SyncEntityData packet) {
            ChatBox.PLATFORM.runOnClient(() -> ChatBoxUtil.setChatTargets(packet.entities));
        }
    }
    *///?}

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
