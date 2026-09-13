package com.zhenshiz.chatbox.data;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class ChatBoxSavedData extends SavedData {
    public static final String TRIGGER_COUNTS = "triggerCounts";
    public static final String GROUPS = "groups";
    private final Map<UUID, Map<ResourceLocation, Integer>> maxTriggerCounts = new HashMap<>();
    private final Map<UUID, Set<UUID>> groups = new HashMap<>();
    private final ServerLevel world;

    public static Factory<ChatBoxSavedData> factory(ServerLevel world) {
        return new Factory<>(() -> new ChatBoxSavedData(world), (nbt, r) -> fromNbt(world, nbt), null);
    }

    public ChatBoxSavedData(ServerLevel world) {
        this.world = world;
    }

    public int getPlayerMaxTriggerCount(ServerPlayer player, ResourceLocation rl) {
        var counts = maxTriggerCounts.getOrDefault(player.getUUID(), Map.of());
        var dialogues = ChatBoxDialoguesLoader.parsedDialogues.get(rl);
        return counts.getOrDefault(rl, dialogues == null ? -1 : dialogues.maxTriggerCount);
    }

    public void setPlayerMaxTriggerCount(ServerPlayer player, ResourceLocation rl, int count) {
        var counts = maxTriggerCounts.getOrDefault(player.getUUID(), new HashMap<>());
        counts.put(rl, count);
        maxTriggerCounts.put(player.getUUID(), counts);
        setDirty();
    }

    public void resetPlayerMaxTriggerCount(ServerPlayer player) {
        maxTriggerCounts.remove(player.getUUID());
        setDirty();
    }

    public boolean isPlayerLeader(ServerPlayer player) {return groups.containsKey(player.getUUID());}

    public boolean isPlayerInGroup(ServerPlayer player) {
        for (var group : groups.values()) {
            if (group.contains(player.getUUID())) return true;
        }
        return false;
    }

    public UUID getPlayerLeader(ServerPlayer player) {
        for (var entry : groups.entrySet()) {
            if (entry.getValue().contains(player.getUUID())) return entry.getKey();
        }
        return null;
    }

    public Set<ServerPlayer> getMembers(ServerPlayer leader) {
        var uuids = groups.getOrDefault(leader.getUUID(), new HashSet<>());
        var members = new HashSet<ServerPlayer>();
        for (var uuid : uuids) {
            var player = world.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) members.add(player);
        }
        return members;
    }

    public List<Component> addPlayerToGroup(ServerPlayer player, Collection<ServerPlayer> members) {
        List<Component> info = new ArrayList<>();
        if (!isPlayerLeader(player) && isPlayerInGroup(player)) {
            info.add(Component.translatable("chatbox.group.add.fail", player.getDisplayName()).withStyle(ChatFormatting.RED));
            return info;
        }
        var leader = player.getUUID();
        var uuids = groups.getOrDefault(leader, new HashSet<>());
        for (var member : members) {
            UUID uuid = member.getUUID();
            if (!uuids.contains(uuid) && isPlayerInGroup(member)) {
                info.add(Component.translatable("chatbox.group.join.fail", member.getDisplayName()).withStyle(ChatFormatting.RED));
            } else {
                uuids.add(uuid);
                info.add(Component.translatable("chatbox.group.join.success", member.getDisplayName(), player.getDisplayName()));
            }
        }
        uuids.add(leader);
        groups.put(leader, uuids);
        setDirty();
        return info;
    }

    public Component removePlayerFromGroup(ServerPlayer player) {
        var leader = getPlayerLeader(player);
        if (leader == null) {
            return Component.translatable("chatbox.group.notin", player.getDisplayName()).withStyle(ChatFormatting.RED);
        }
        if (player.getUUID().equals(leader)) {
            groups.remove(leader);
            setDirty();
            return Component.translatable("chatbox.group.remove", player.getDisplayName());
        }
        var uuids = groups.get(leader);
        uuids.remove(player.getUUID());
        groups.put(leader, uuids);
        setDirty();
        return Component.translatable("chatbox.group.quit", player.getDisplayName());
    }

    public Component setLeader(ServerPlayer player) {
        var leader = getPlayerLeader(player);
        if (leader == null) {
            return Component.translatable("chatbox.group.notin", player.getDisplayName()).withStyle(ChatFormatting.RED);
        }
        groups.put(player.getUUID(), groups.remove(leader));
        setDirty();
        return Component.translatable("chatbox.group.setLeader", player.getDisplayName());
    }

    public Component clearGroup() {
        groups.clear();
        setDirty();
        return Component.translatable("chatbox.group.clear");
    }

    public static ChatBoxSavedData fromNbt(ServerLevel world, CompoundTag nbt) {
        var data = new ChatBoxSavedData(world);
        var triggerCounts = nbt.getCompound(TRIGGER_COUNTS);
        for (var player : triggerCounts.getAllKeys()) {
            var counts = new HashMap<ResourceLocation, Integer>();
            var playerCounts = triggerCounts.getCompound(player);
            for (var key : playerCounts.getAllKeys()) {
                try {
                    var rl = ResourceLocation.tryParse(key);
                    int num = playerCounts.getInt(key);
                    counts.put(rl, num);
                } catch (Exception ignored) {}
            }
            data.maxTriggerCounts.put(UUID.fromString(player), counts);
        }

        var groups = nbt.getCompound(GROUPS);
        for (var key : groups.getAllKeys()) {
            try {
                var listTag = groups.getList(key, 8);
                var uuids = new HashSet<UUID>();
                for (var tag : listTag) uuids.add(UUID.fromString(tag.getAsString()));
                data.groups.put(UUID.fromString(key), uuids);
            } catch (Exception ignored) {}
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        var triggerCounts = new CompoundTag();
        for (var entry : maxTriggerCounts.entrySet()) {
            var player = entry.getKey();
            var playerCounts = entry.getValue();
            var compoundTag = new CompoundTag();
            for (var entry1 : playerCounts.entrySet()) {
                compoundTag.putInt(entry1.getKey().toString(), entry1.getValue());
            }
            triggerCounts.put(player.toString(), compoundTag);
        }
        nbt.put(TRIGGER_COUNTS, triggerCounts);

        var groupsTag = new CompoundTag();
        for (var entry : groups.entrySet()) {
            var leader = entry.getKey();
            var uuids = entry.getValue();
            var listTag = new ListTag();
            for (var uuid : uuids) listTag.add(StringTag.valueOf(uuid.toString()));
            groupsTag.put(leader.toString(), listTag);
        }
        nbt.put(GROUPS, groupsTag);
        return nbt;
    }
}
