package com.zhenshiz.chatbox.event;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.payload.s2c.ClientChatBoxPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ChatBox.MOD_ID)
public class ChatBoxSettingLoader {

    @SubscribeEvent
    public static void chatBoxLoader(AddReloadListenerEvent event) {
        event.addListener(ChatBoxThemeLoader.INSTANCE);
        event.addListener(ChatBoxDialoguesLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void initializeChatBoxScreen(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        //发包到客户端
        player.connection.send(new ClientChatBoxPayload.AllChatBoxThemeToClient(ChatBoxSettingLoader.cutString(ChatBoxThemeLoader.INSTANCE.themeMap)));
        player.connection.send(new ClientChatBoxPayload.AllChatBoxDialoguesToClient(ChatBoxSettingLoader.cutString(ChatBoxDialoguesLoader.INSTANCE.dialoguesMap)));
    }

    @SubscribeEvent
    public static void initializeServer(ServerStartingEvent event) {
        ChatBoxDialoguesLoader.loadCriteria(event.getServer());
    }

    //由于字符串长度的限制为32767，所以需要把字符串分割成多个字符串，然后再发送给客户端Add commentMore actions
    private static final int STRING_SIZE_LIMIT = 32000;

    public static Map<ResourceLocation, List<String>> cutString(Map<ResourceLocation, String> map) {
        Map<ResourceLocation, List<String>> result = new HashMap<>();
        for (var entry : map.entrySet()) {
            ResourceLocation rl = entry.getKey();
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
