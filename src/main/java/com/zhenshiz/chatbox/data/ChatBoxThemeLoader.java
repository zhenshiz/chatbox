package com.zhenshiz.chatbox.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ChatBoxThemeLoader extends ChatBoxDataLoader {
    public static final Map<ResourceLocation, String> themeMap = new HashMap<>();

    public ChatBoxThemeLoader() {
        super("chatbox/theme");
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, String> resourceLocationStringMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        themeMap.clear();
        themeMap.putAll(resourceLocationStringMap);
    }
}
