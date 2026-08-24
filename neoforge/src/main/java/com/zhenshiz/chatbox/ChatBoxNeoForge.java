package com.zhenshiz.chatbox;

import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.neoforge.platform.NeoForgePlatformHelper;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(ChatBox.MOD_ID)
public class ChatBoxNeoForge {

    public ChatBoxNeoForge(ModContainer modContainer, Dist dist) {
        ChatBox.PLATFORM = new NeoForgePlatformHelper();
        ChatBox.init();
        if (dist.isClient()) {
            ChatBoxClient.init();
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (_, parent) -> AutoConfigClient.getConfigScreen(Config.class, parent).get());
        }
    }
}
