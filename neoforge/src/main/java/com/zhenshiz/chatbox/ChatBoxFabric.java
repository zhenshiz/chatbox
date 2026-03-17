package com.zhenshiz.chatbox;

import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.fabric.Network;
import com.zhenshiz.chatbox.fabric.SettingLoader;
import com.zhenshiz.chatbox.fabric.platform.FabricPlatformHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.Level;

public class ChatBoxFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ChatBox.PLATFORM = new FabricPlatformHelper();
        ChatBox.init();
        SettingLoader.chatBoxLoader();
        Network.register();
        CommandRegistrationCallback.EVENT.register(ChatBoxCommand::register);
        ServerLevelEvents.LOAD.register((server, world) -> {
            //只需要保存在主世界的data目录下即可
            if (world.dimension() == Level.OVERWORLD) ChatBox.setTriggerCounts(world.getDataStorage().computeIfAbsent(ChatBoxTriggerCount.getType()));
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((s, manager, bl) -> {
            ChatBoxDialoguesLoader.loadCriteria(s);
            s.getPlayerList().getPlayers().forEach(SettingLoader::initializeChatBoxScreen);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(ChatBoxDialoguesLoader::loadCriteria);
    }
}
