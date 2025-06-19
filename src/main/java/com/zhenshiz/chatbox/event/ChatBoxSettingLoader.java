package com.zhenshiz.chatbox.event;

import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;

public class ChatBoxSettingLoader {

    public static void chatBoxLoader() {
        ResourceManagerHelper resourceManagerHelper = ResourceManagerHelper.get(PackType.SERVER_DATA);
        resourceManagerHelper.registerReloadListener(ChatBoxThemeLoader.INSTANCE);
        resourceManagerHelper.registerReloadListener(ChatBoxDialoguesLoader.INSTANCE);
    }

    public static void initializeChatBoxScreen(ServerPlayer player) {
        //发包到客户端
        ServerPlayNetworking.send(player, new ChatBoxPayload.AllChatBoxThemeToClient(ChatBoxThemeLoader.INSTANCE.themeMap));
        ServerPlayNetworking.send(player, new ChatBoxPayload.AllChatBoxDialoguesToClient(ChatBoxDialoguesLoader.INSTANCE.dialoguesMap));
    }
}
