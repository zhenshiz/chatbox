package com.zhenshiz.chatbox.event;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

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
        player.connection.send(new ChatBoxPayload.AllChatBoxThemeToClient(ChatBoxThemeLoader.INSTANCE.themeMap));
        player.connection.send(new ChatBoxPayload.AllChatBoxDialoguesToClient(ChatBoxDialoguesLoader.INSTANCE.dialoguesMap));
    }
}
