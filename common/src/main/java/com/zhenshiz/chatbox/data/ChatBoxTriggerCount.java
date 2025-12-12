package com.zhenshiz.chatbox.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader.defaultMaxTriggerCount;

public class ChatBoxTriggerCount extends SavedData {
    private static final Codec<ChatBoxTriggerCount> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.unboundedMap(Identifier.CODEC, Codec.INT))
                                    .fieldOf("MaxTriggerCounts")
                                    .forGetter(o -> o.maxTriggerCounts)
                    )
                    .apply(instance, ChatBoxTriggerCount::new)
    );
    private final Map<UUID, Map<Identifier, Integer>> maxTriggerCounts = new HashMap<>();

    public static SavedDataType<ChatBoxTriggerCount> getType() {
        return new SavedDataType<>("chatbox_trigger_count", ChatBoxTriggerCount::new, CODEC, null);
    }

    public ChatBoxTriggerCount() {setDirty();}
    public ChatBoxTriggerCount(Map<UUID, Map<Identifier, Integer>> counts) {
        // Codec解析出来的是不可修改的map，需要复制一下
        for (Map.Entry<UUID, Map<Identifier, Integer>> entry : counts.entrySet()) {
            maxTriggerCounts.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        setDirty();
    }

    public int getPlayerMaxTriggerCount(ServerPlayer player, Identifier rl) {
        setDirty();
        Map<Identifier, Integer> counts = maxTriggerCounts.computeIfAbsent(player.getUUID(), k -> defaultMaxTriggerCount);
        return counts.getOrDefault(rl, defaultMaxTriggerCount.getOrDefault(rl, 0));
    }

    public void setPlayerMaxTriggerCount(ServerPlayer player, Identifier rl, int count) {
        Map<Identifier, Integer> counts = maxTriggerCounts.computeIfAbsent(player.getUUID(), k -> defaultMaxTriggerCount);
        counts.put(rl, count);
        maxTriggerCounts.put(player.getUUID(), counts);
        setDirty();
    }

    public void resetPlayerMaxTriggerCount(ServerPlayer player) {
        maxTriggerCounts.remove(player.getUUID());
        setDirty();
    }
}
