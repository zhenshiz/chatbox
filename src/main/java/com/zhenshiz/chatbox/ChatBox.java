package com.zhenshiz.chatbox;

import com.mojang.logging.LogUtils;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.event.ChatBoxSettingLoader;
import com.zhenshiz.chatbox.network.server.Packets;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class ChatBox implements ModInitializer {
    public static final String MOD_ID = "chatbox";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static MinecraftServer server;
    @Getter
    private static ChatBoxTriggerCount triggerCounts;

    @Override
    public void onInitialize() {
        ChatBoxSettingLoader.chatBoxLoader();
        Packets.register();
        CommandRegistrationCallback.EVENT.register(ChatBoxCommand::register);
        ServerTickEvents.END_SERVER_TICK.register(minecraftServer -> server = minecraftServer);
        ServerWorldEvents.LOAD.register((server, world) -> {
            //只需要保存在主世界的data目录下即可
            if (world.dimension() == Level.OVERWORLD) triggerCounts = world.getDataStorage().computeIfAbsent(ChatBoxTriggerCount.factory(world), "chatbox_trigger_count");
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((s, manager, bl) -> ChatBoxDialoguesLoader.loadCriteria(s));
        ServerLifecycleEvents.SERVER_STARTED.register(ChatBoxDialoguesLoader::loadCriteria);
    }

/*    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        //只需要保存在主世界的data目录下即可
        if (levelAccessor instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) triggerCounts = world.getDataStorage().computeIfAbsent(ChatBoxTriggerCount.factory(world), "chatbox_trigger_count");
    }*/

    public static ResourceLocation ResourceLocationMod(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
