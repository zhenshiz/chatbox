package com.zhenshiz.chatbox.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zhenshiz.chatbox.ChatBox;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ChatBoxTriggerCount {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ChatBox.MOD_ID);

    public static final Supplier<AttachmentType<MaxTriggerCount>> MAX_TRIGGER_COUNT = ATTACHMENT_TYPES.register(
            "max_trigger_count",
            () -> AttachmentType.builder(() -> new MaxTriggerCount())
                    .serialize(MaxTriggerCount.CODEC)
                    .build()
    );

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MaxTriggerCount {
        public Map<String, Integer> triggerCounts = new HashMap<>();

        public static final Codec<MaxTriggerCount> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                                Codec.unboundedMap(Codec.STRING, Codec.INT)
                                        .optionalFieldOf("max_trigger_count", new HashMap<>())
                                        .forGetter(mtc -> mtc.triggerCounts)
                        )
                        .apply(instance, MaxTriggerCount::new)
        );

        public static final StreamCodec<ByteBuf, MaxTriggerCount> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
                MaxTriggerCount::getTriggerCounts,
                MaxTriggerCount::new
        );
    }
}
