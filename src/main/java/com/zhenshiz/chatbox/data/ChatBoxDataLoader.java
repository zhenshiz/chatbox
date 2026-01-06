package com.zhenshiz.chatbox.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zhenshiz.chatbox.ChatBox;
import me.shedaniel.cloth.clothconfig.shadowed.org.yaml.snakeyaml.Yaml;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ChatBoxDataLoader extends SimplePreparableReloadListener<Map<ResourceLocation, String>> {
    private final String directory;

    public ChatBoxDataLoader(String directory) {
        this.directory = directory;
    }

    @Override
    protected @NotNull Map<ResourceLocation, String> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, String> map = new HashMap<>();

        FileToIdConverter yamlConverter = new FileToIdConverter(directory, ".yaml");
        for (var entry : yamlConverter.listMatchingResources(resourceManager).entrySet()) {
            var resourceLocation = entry.getKey();
            var rl = yamlConverter.fileToId(resourceLocation);

            try (var resource = entry.getValue().openAsReader()) {
                var yaml = yamlToJson(resource.lines().collect(Collectors.joining("\n")));
                map.put(rl, yaml);
            } catch (IOException e) {
                ChatBox.LOGGER.error("Error loading data from {}: {}", rl, e.getMessage());
            }
        }

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

    private static final Yaml YAML = new Yaml();
    private static final Gson GSON = new GsonBuilder().create();

    public static String yamlToJson(String yamlContent) {
        Map<String, Object> yamlMap = YAML.load(yamlContent);
        return GSON.toJson(yamlMap);
    }
}
