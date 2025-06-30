package com.zhenshiz.chatbox.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.zhenshiz.chatbox.ChatBox;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ChatBoxThemeLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
    private static final Gson GSON =
            (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    public static final ChatBoxThemeLoader INSTANCE = new ChatBoxThemeLoader();
    public final Map<ResourceLocation, String> themeMap = new HashMap<>();

    public ChatBoxThemeLoader() {
        super(GSON, "chatbox/theme");
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        themeMap.clear();
        resourceManager.listPacks().forEach(packResources -> {
            Set<String> namespaces = packResources.getNamespaces(PackType.SERVER_DATA);
            namespaces.forEach(namespace -> packResources.listResources(PackType.SERVER_DATA, namespace, "chatbox/theme", ((resourceLocation, inputStreamIoSupplier) -> {
                String path = resourceLocation.getPath();
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(namespace, path.substring("chatbox/theme/".length(), path.length() - ".json".length()));
                JsonElement jsonElement = resourceLocationJsonElementMap.get(rl);
                if (jsonElement != null) themeMap.put(rl, jsonElement.toString());
            })));
        });
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(ChatBox.MOD_ID, "chatbox/theme");
    }
}
