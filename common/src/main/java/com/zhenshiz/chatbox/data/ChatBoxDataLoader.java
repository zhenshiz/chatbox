package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ChatBoxDataLoader extends SimplePreparableReloadListener<Map<Identifier, String>> {
    private final String directory;

    public ChatBoxDataLoader(String directory) {
        this.directory = directory;
    }

    @Override
    protected @NotNull Map<Identifier, String> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, String> map = new HashMap<>();

        FileToIdConverter jsonConverter = FileToIdConverter.json(directory);
        for (var entry : jsonConverter.listMatchingResources(resourceManager).entrySet()) {
            var resourceLocation = entry.getKey();
            var rl = jsonConverter.fileToId(resourceLocation);

            try (var resource = entry.getValue().openAsReader()) {
                var json = resource.lines().collect(Collectors.joining("\n"));
                map.put(rl, json);
            } catch (IOException e) {
                ChatBox.LOGGER.error("Error loading data from {}: {}", rl, e.getMessage());
            }
        }

        return map;
    }
}
