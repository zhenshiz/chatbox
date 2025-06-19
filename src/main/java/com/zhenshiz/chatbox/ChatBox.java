package com.zhenshiz.chatbox;

import com.mojang.logging.LogUtils;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.event.ChatBoxSettingLoader;
import com.zhenshiz.chatbox.network.client.ChatBoxClient;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public class ChatBox implements ModInitializer {
    public static final String MOD_ID = "chatbox";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static MinecraftServer server;
    public static Config conf;

    @Override
    public void onInitialize() {
        ChatBoxSettingLoader.chatBoxLoader();
        ChatBoxClient.register();
        CommandRegistrationCallback.EVENT.register(ChatBoxCommand::register);
        ServerTickEvents.END_SERVER_TICK.register(minecraftServer -> server = minecraftServer);
        AutoConfig.register(Config.class, Toml4jConfigSerializer::new);
        conf = AutoConfig.getConfigHolder(Config.class).getConfig();
    }

    public static ResourceLocation ResourceLocationMod(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
