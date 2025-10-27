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
    public record OpenScreenPayload(ResourceLocation dialogues, String group,
                                    int index, String targets) implements CustomPacketPayload {
        public static final Type<OpenScreenPayload> TYPE = new Type<>(ChatBox.ResourceLocationMod("open_screen"));
        public static final StreamCodec<FriendlyByteBuf, OpenScreenPayload> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                OpenScreenPayload::dialogues,
                ByteBufCodecs.STRING_UTF8,
                OpenScreenPayload::group,
                ByteBufCodecs.INT,
                OpenScreenPayload::index,
                ByteBufCodecs.STRING_UTF8,
                OpenScreenPayload::targets,
                OpenScreenPayload::new
        );

        public static void execute(OpenScreenPayload payload, IPayloadContext context) {
            ChatBoxCommandUtil.clientSkipDialogues(payload.dialogues(), payload.group(), payload.index());
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AllChatBoxThemeToClient(
            Map<ResourceLocation, List<String>> themeMap) implements CustomPacketPayload {
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

        public static void execute(AllChatBoxThemeToClient payload, IPayloadContext context) {
            ChatBoxUtil.setTheme(mergeString(payload.themeMap()));
            if (ChatBoxUtil.themeResourceLocation != null) {
                ResourceLocation theme = ResourceLocation.tryParse(ChatBoxUtil.themeResourceLocation);
                if (theme != null) {
                    ChatBoxUtil.toggleTheme(theme);
                }
            }
            ;
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AllChatBoxDialoguesToClient(
            Map<ResourceLocation, List<String>> dialoguesMap) implements CustomPacketPayload {
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

        public static void execute(AllChatBoxDialoguesToClient payload, IPayloadContext context) {
            ChatBoxUtil.setDialogues(mergeString(payload.dialoguesMap()));
        }

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

        public static void execute(SetMaxTriggerCount payload, IPayloadContext context) {
            ChatBoxCommandUtil.clientSetMaxTriggerCount(payload.resourceLocation(), payload.maxTriggerCount());
        }

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

        public static void execute(ResetMaxTriggerCount payload, IPayloadContext context) {
            ChatBoxCommandUtil.clientResetMaxTriggerCount();
        }

        @Override
        public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncEntityData(LinkedHashMap<Integer, CompoundTag> entities) implements CustomPacketPayload {
        public static final Type<SyncEntityData> TYPE = new Type<>(ChatBox.ResourceLocationMod("sync_entity_data"));
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
