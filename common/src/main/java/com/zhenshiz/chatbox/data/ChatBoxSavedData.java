package com.zhenshiz.chatbox.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class ChatBoxSavedData extends SavedData {
    public static final String TRIGGER_COUNTS = "triggerCounts";
    public static final String GROUPS = "groups";
    private static final Codec<ChatBoxSavedData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.unboundedMap(Identifier.CODEC, Codec.INT))
                                    .fieldOf(TRIGGER_COUNTS).forGetter(o -> o.maxTriggerCounts),
                            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.list(UUIDUtil.STRING_CODEC))
                                    .fieldOf(GROUPS).forGetter(ChatBoxSavedData::groups)
                    ).apply(instance, ChatBoxSavedData::new)
    );
    private final Map<UUID, Map<Identifier, Integer>> maxTriggerCounts = new HashMap<>();
    private final Map<UUID, Set<UUID>> groups = new HashMap<>();

    private Map<UUID, List<UUID>> groups() {
        var map = new HashMap<UUID, List<UUID>>();
        for (Map.Entry<UUID, Set<UUID>> entry : groups.entrySet()) {
            map.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return map;
    }

    public static SavedDataType<ChatBoxSavedData> getType() {
        return new SavedDataType<>(ChatBox.id("chatbox_saved_data"), ChatBoxSavedData::new, CODEC, null);
    }

    public ChatBoxSavedData() {setDirty();}
    public ChatBoxSavedData(Map<UUID, Map<Identifier, Integer>> counts, Map<UUID, List<UUID>> groups) {
        // Codec解析出来的是不可修改的map，需要复制一下
        for (var entry : counts.entrySet()) {
            maxTriggerCounts.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        for (var entry : groups.entrySet()) {
            this.groups.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        setDirty();
    }

    public int getPlayerMaxTriggerCount(ServerPlayer player, Identifier rl) {
        var counts = maxTriggerCounts.getOrDefault(player.getUUID(), Map.of());
        var dialogues = ChatBoxDialoguesLoader.parsedDialogues.get(rl);
        return counts.getOrDefault(rl, dialogues == null ? -1 : dialogues.maxTriggerCount);
    }

    public void setPlayerMaxTriggerCount(ServerPlayer player, Identifier rl, int count) {
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
        if (ChatBox.server != null) for (var uuid : uuids) {
            ServerPlayer player = ChatBox.server.getPlayerList().getPlayer(uuid);
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
}
