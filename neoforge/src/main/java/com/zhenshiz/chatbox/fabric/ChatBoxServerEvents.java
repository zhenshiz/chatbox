package com.zhenshiz.chatbox.fabric;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxSavedData;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.fabric.event.SkipChatEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatBoxServerEvents {

    private static void loadSavedData(MinecraftServer server, ServerLevel world) {
        //只需要保存在主世界的data目录下即可
        if (world.dimension() == Level.OVERWORLD)
            ChatBox.setSavedData(world.getDataStorage().computeIfAbsent(ChatBoxSavedData.getType()));
    }

    public static void init() {
        ResourceLoader loader = ResourceLoader.get(PackType.SERVER_DATA);
        loader.registerReloadListener(ChatBox.id("chatbox/dialogues"), new ChatBoxDialoguesLoader());
        loader.registerReloadListener(ChatBox.id("chatbox/theme"), new ChatBoxThemeLoader());
        CommandRegistrationCallback.EVENT.register(ChatBoxCommand::register);
        ServerLevelEvents.LOAD.register(ChatBoxServerEvents::loadSavedData);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((s, _, _) -> {
            ChatBoxDialoguesLoader.loadCriteria(s);
            s.getPlayerList().getPlayers().forEach(ChatBoxServerEvents::initializeChatBoxScreen);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(ChatBoxDialoguesLoader::loadCriteria);
        SkipChatEvent.EVENT.register(ChatBoxServerEvents::onSkipChat);
    }

    public static void initializeChatBoxScreen(ServerPlayer player) {
        //玩家进入以及重载数据包后，发包到客户端
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.ChatBoxDataToClient("theme", cutString(ChatBoxThemeLoader.themeMap)));
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.ChatBoxDataToClient("dialogues", cutString(ChatBoxDialoguesLoader.dialoguesMap)));
    }

    //由于字符串长度的限制为32767，所以需要把字符串分割成多个字符串，然后再发送给客户端
    private static final int STRING_SIZE_LIMIT = 32000;

    private static Map<Identifier, List<String>> cutString(Map<Identifier, String> map) {
        Map<Identifier, List<String>> result = new HashMap<>();
        for (var entry : map.entrySet()) {
            Identifier rl = entry.getKey();
            String data = entry.getValue();
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < data.length(); i += STRING_SIZE_LIMIT) {
                parts.add(data.substring(i, Math.min(i + STRING_SIZE_LIMIT, data.length())));
            }
            result.put(rl, parts);
        }
        return result;
    }

    private static void onSkipChat(Player p, Identifier rl, String group, int index, List<Entity> targets) {
        if (!(p instanceof ServerPlayer player)) return;
        var data = ChatBox.getSavedData();
        if (!data.isPlayerLeader(player)) return;
        for (var member : data.getMembers(player)) {
            if (member == player) continue;
            ChatBoxCommandUtil.serverSkipDialogues(member, rl, group, index, targets);
            ChatBoxCommandUtil.setBlockInput(member, index != -1);
        }
    }
}
