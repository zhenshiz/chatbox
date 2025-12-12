package com.zhenshiz.chatbox.event;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ChatBox.MOD_ID)
public class ChatBoxSettingLoader {

    @SubscribeEvent
    public static void chatBoxLoader(AddServerReloadListenersEvent event) {
        event.addListener(ChatBox.id("chatbox/theme"), new ChatBoxThemeLoader());
        event.addListener(ChatBox.id("chatbox/dialogues"), new ChatBoxDialoguesLoader());
    }

    @SubscribeEvent
    public static void initializeChatBoxScreen(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        //发包到客户端
        if (player != null) {
            initializeChatBoxScreen(player);
        } else {
            event.getPlayerList().getPlayers().forEach(ChatBoxSettingLoader::initializeChatBoxScreen);
        }
    }

    private static void initializeChatBoxScreen(ServerPlayer player) {
        //玩家进入以及重载数据包后，发包到客户端
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.AllChatBoxThemeToClient(cutString(ChatBoxThemeLoader.themeMap)));
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.AllChatBoxDialoguesToClient(cutString(ChatBoxDialoguesLoader.dialoguesMap)));
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
}
