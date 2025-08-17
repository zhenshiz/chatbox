package com.zhenshiz.chatbox;

import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.event.ChatBoxSettingLoader;
import com.zhenshiz.chatbox.network.NetworkFabric;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.level.Level;

public class ChatBoxFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ChatBoxSettingLoader.chatBoxLoader();
        NetworkFabric.registerServerHandlers();
        CommandRegistrationCallback.EVENT.register(ChatBoxCommand::register);
        ServerWorldEvents.LOAD.register((server, world) -> {
            //只需要保存在主世界的data目录下即可
            if (world.dimension() == Level.OVERWORLD) ChatBox.setTriggerCounts(world.getDataStorage().computeIfAbsent(nbt -> ChatBoxTriggerCount.fromNbt(world, nbt), () -> new ChatBoxTriggerCount(world), "chatbox_trigger_count"));
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((s, manager, bl) -> s.getPlayerList().getPlayers().forEach(ChatBoxSettingLoader::initializeChatBoxScreen));
    }
}
