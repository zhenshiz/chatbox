package com.zhenshiz.chatbox.data;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ChatBoxThemeLoader extends ChatBoxDataLoader {
    public static final Map<Identifier, String> themeMap = new HashMap<>();

    public ChatBoxThemeLoader() {
        super("chatbox/theme");
    }

    @Override
    protected void apply(@NotNull Map<Identifier, String> map, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        themeMap.clear();
        themeMap.putAll(map);
    }
}
