package com.zhenshiz.chatbox;

import com.zhenshiz.chatbox.fabric.ChatBoxServerEvents;
import com.zhenshiz.chatbox.fabric.Network;
import com.zhenshiz.chatbox.fabric.platform.FabricPlatformHelper;
import net.fabricmc.api.ModInitializer;

public class ChatBoxFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ChatBox.PLATFORM = new FabricPlatformHelper();
        ChatBox.init();
        ChatBoxServerEvents.init();
        Network.register();
    }
}
