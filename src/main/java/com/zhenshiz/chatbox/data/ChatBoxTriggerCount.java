package com.zhenshiz.chatbox.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader.defaultMaxTriggerCount;

public class ChatBoxTriggerCount extends SavedData {
    private final Map<UUID, Map<ResourceLocation, Integer>> maxTriggerCounts = new HashMap<>();
    private final ServerLevel world;

    public static SavedData.Factory<ChatBoxTriggerCount> factory(ServerLevel world) {
        return new SavedData.Factory<>(() -> new ChatBoxTriggerCount(world), (nbt, r) -> fromNbt(world, nbt), null);
    }

    public ChatBoxTriggerCount(ServerLevel world) {
        this.world = world;
    }

    public int getPlayerMaxTriggerCount(ServerPlayer player, ResourceLocation rl) {
        setDirty();
        Map<ResourceLocation, Integer> counts = maxTriggerCounts.computeIfAbsent(player.getUUID(), k -> defaultMaxTriggerCount);
        return counts.getOrDefault(rl, 0);
    }

    public void setPlayerMaxTriggerCount(ServerPlayer player, ResourceLocation rl, int count) {
        Map<ResourceLocation, Integer> counts = maxTriggerCounts.computeIfAbsent(player.getUUID(), k -> defaultMaxTriggerCount);
        counts.put(rl, count);
        maxTriggerCounts.put(player.getUUID(), counts);
        setDirty();
    }

    public void resetPlayerMaxTriggerCount(ServerPlayer player) {
        maxTriggerCounts.remove(player.getUUID());
        setDirty();
    }

    public static ChatBoxTriggerCount fromNbt(ServerLevel world, CompoundTag nbt) {
        ChatBoxTriggerCount count = new ChatBoxTriggerCount(world);
        for (var player : nbt.getAllKeys()) {
            UUID uuid = UUID.fromString(player);
            Map<ResourceLocation, Integer> counts = new HashMap<>();
            ListTag listTag = nbt.getList(player, 10);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag compoundTag = listTag.getCompound(i);
                ResourceLocation rl = ResourceLocation.tryParse(compoundTag.getString("Dialogue"));
                int num = compoundTag.getInt("MaxTriggerCount");
                counts.put(rl, num);
            }
            count.maxTriggerCounts.put(uuid, counts);
        }
        return count;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        for (var entry : maxTriggerCounts.entrySet()) {
            ListTag listTag = new ListTag();
            UUID player = entry.getKey();
            Map<ResourceLocation, Integer> playerCounts = entry.getValue();
            for (var entry1 : playerCounts.entrySet()) {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putString("Dialogue", entry1.getKey().toString());
                compoundTag.putInt("MaxTriggerCount", entry1.getValue());
                listTag.add(compoundTag);
            }
            nbt.put(player.toString(), listTag);
        }
        return nbt;
    }
}
