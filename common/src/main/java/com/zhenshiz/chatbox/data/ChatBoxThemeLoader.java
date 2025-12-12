package com.zhenshiz.chatbox.data;

import com.google.gson.JsonElement;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ChatBoxThemeLoader extends SimpleJsonDataLoader {
    public static final Map<Identifier, String> themeMap = new HashMap<>();

    public ChatBoxThemeLoader() {
        super(FileToIdConverter.json("chatbox/theme"));
    }

    @Override
    protected void apply(@NotNull Map<Identifier, JsonElement> IdentifierJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        themeMap.clear();
        IdentifierJsonElementMap.forEach((Identifier, jsonElement) -> themeMap.put(Identifier, jsonElement.toString()));
    }
}
