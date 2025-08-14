package com.zhenshiz.chatbox.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public abstract class SimpleJsonDataLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final FileToIdConverter finder;

    public SimpleJsonDataLoader(FileToIdConverter finder) {
        this.finder = finder;
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> results = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : finder.listMatchingResources(manager).entrySet()) {
            ResourceLocation identifier = entry.getKey();
            ResourceLocation identifier2 = finder.fileToId(identifier);

            try {
                Reader reader = entry.getValue().openAsReader();
                JsonElement jsonElement = StrictJsonParser.parse(reader);
                if (results.put(identifier2, jsonElement) == null) continue;
                throw new IllegalStateException("Duplicate data file ignored with ID " + identifier2);
            } catch (JsonParseException | IOException | IllegalArgumentException exception) {
                LOGGER.error("Couldn't parse data file {} from {}", identifier2, identifier, exception);
            }
        }
        return results;
    }
}
