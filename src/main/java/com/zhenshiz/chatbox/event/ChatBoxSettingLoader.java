package com.zhenshiz.chatbox.event;

import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatBoxSettingLoader {

    public static void chatBoxLoader() {
        ResourceManagerHelper resourceManagerHelper = ResourceManagerHelper.get(PackType.SERVER_DATA);
        resourceManagerHelper.registerReloadListener(ChatBoxThemeLoader.INSTANCE);
        resourceManagerHelper.registerReloadListener(ChatBoxDialoguesLoader.INSTANCE);
    }

    public static void initializeChatBoxScreen(ServerPlayer player) {
        //玩家进入以及重载数据包后，发包到客户端
        ServerPlayNetworking.send(player, new ChatBoxPayload.AllChatBoxThemeToClient(cutString(ChatBoxThemeLoader.INSTANCE.themeMap)));
        ServerPlayNetworking.send(player, new ChatBoxPayload.AllChatBoxDialoguesToClient(cutString(ChatBoxDialoguesLoader.INSTANCE.dialoguesMap)));
    }

    //由于字符串长度的限制为32767，所以需要把字符串分割成多个字符串，然后再发送给客户端
    private static final int STRING_SIZE_LIMIT = 32000;
    //经过测试，单人游戏正常运行，多人游戏无法打开我的对话框，原因未知
    private static Map<ResourceLocation, List<String>> cutString(Map<ResourceLocation, String> map) {
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
