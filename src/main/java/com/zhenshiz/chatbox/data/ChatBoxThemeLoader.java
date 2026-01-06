package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.event.ChatBoxSettingLoader;
import com.zhenshiz.chatbox.network.s2c.ClientChatBoxPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ChatBoxThemeLoader extends ChatBoxDataLoader {
    public static final Map<ResourceLocation, String> themeMap = new HashMap<>();

    public ChatBoxThemeLoader() {
        super("chatbox/theme");
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, String> map, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        themeMap.clear();
        themeMap.putAll(map);

        //给所有玩家发包
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(serverPlayer -> serverPlayer.connection.send(new ClientChatBoxPayload.ChatBoxDataToClient("theme", ChatBoxSettingLoader.cutString(themeMap))));
        }
    }
}
