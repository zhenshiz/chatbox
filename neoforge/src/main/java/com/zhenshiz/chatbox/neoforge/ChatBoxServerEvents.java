package com.zhenshiz.chatbox.neoforge;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxSavedData;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.neoforge.event.SkipChatEvent;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ChatBox.MOD_ID)
public class ChatBoxServerEvents {

    @SubscribeEvent
    public static void chatBoxLoader(AddServerReloadListenersEvent event) {
        event.addListener(ChatBox.id("chatbox/theme"), new ChatBoxThemeLoader());
        event.addListener(ChatBox.id("chatbox/dialogues"), new ChatBoxDialoguesLoader());
    }

    @SubscribeEvent
    public static void initializeChatBoxScreen(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        //发包到客户端
        if (player != null) initializeChatBoxScreen(player);
        else event.getPlayerList().getPlayers().forEach(ChatBoxServerEvents::initializeChatBoxScreen);
        ChatBoxDialoguesLoader.loadCriteria(event.getPlayerList().getServer());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ChatBoxCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        var levelAccessor = event.getLevel();
        //只需要保存在主世界的data目录下即可
        if (levelAccessor instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) {
            ChatBox.setSavedData(world.getDataStorage().computeIfAbsent(ChatBoxSavedData.getType()));
        }
    }

    @SubscribeEvent
    public static void onSkipChat(SkipChatEvent event) {
        onSkipChat(event.getPlayer(), event.getIdentifier(), event.getGroup(), event.getIndex(), event.getTargets());
    }

    private static void initializeChatBoxScreen(ServerPlayer player) {
        //玩家进入以及重载数据包后，发包到客户端
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.ChatBoxDataToClient("theme", cutString(ChatBoxThemeLoader.themeMap)));
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.ChatBoxDataToClient("dialogues", cutString(ChatBoxDialoguesLoader.dialoguesMap)));
    }

    //由于字符串长度的限制为32767，所以需要把字符串分割成多个字符串，然后再发送给客户端Add commentMore actions
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
