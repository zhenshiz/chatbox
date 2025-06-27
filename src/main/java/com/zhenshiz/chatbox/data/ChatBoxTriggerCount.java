package com.zhenshiz.chatbox.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.payload.s2c.ClientChatBoxPayload;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ChatBoxTriggerCount {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ChatBox.MOD_ID);

    public static final Supplier<AttachmentType<MaxTriggerCount>> MAX_TRIGGER_COUNT = ATTACHMENT_TYPES.register(
            "max_trigger_count",
            () -> AttachmentType.builder(() -> new MaxTriggerCount())
                    .serialize(MaxTriggerCount.CODEC)
                    .copyOnDeath()
                    .build()
    );

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MaxTriggerCount {
        private Map<String, Integer> triggerCounts = new HashMap<>();

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

    @EventBusSubscriber(modid = ChatBox.MOD_ID)
    private static class Event {
        @SubscribeEvent
        public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            Player player = event.getEntity();
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientChatBoxPayload.SetMaxTriggerCountPlus(serverPlayer.getData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT)));
            }
        }

        @SubscribeEvent
        public static void clone(PlayerEvent.Clone event) {
            ServerPlayer original = (ServerPlayer) event.getOriginal();
            ServerPlayer entity = (ServerPlayer) event.getEntity();
            if (event.isWasDeath() && event.getOriginal().hasData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT)) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                scheduler.schedule(() -> entity.connection.send(new ClientChatBoxPayload.SetMaxTriggerCountPlus(original.getData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT))), 1, TimeUnit.MILLISECONDS);
            }
        }
    }
}
