package com.zhenshiz.chatbox.event;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.data.DefaultChatBox;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.world.entity.player.Player;
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
        Player player = event.getEntity();
        ChatBoxScreen chatBoxScreen = ChatBoxUtil.chatBoxScreens.get(player.getUUID());
        if (chatBoxScreen == null) ChatBoxUtil.chatBoxScreens.put(player.getUUID(), DefaultChatBox.getDefaultTheme());
    }
}
