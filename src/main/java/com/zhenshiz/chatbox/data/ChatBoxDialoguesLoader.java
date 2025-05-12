package com.zhenshiz.chatbox.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatBoxDialoguesLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON =
            (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    public static final ChatBoxDialoguesLoader INSTANCE = new ChatBoxDialoguesLoader();
    public final Map<ResourceLocation, JsonElement> dialoguesMap = new HashMap<>();

    public ChatBoxDialoguesLoader() {
        super(GSON, "chatbox/dialogues");
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        dialoguesMap.clear();
        resourceManager.listPacks().forEach(packResources -> {
            Set<String> namespaces = packResources.getNamespaces(PackType.SERVER_DATA);
            namespaces.forEach(namespace -> packResources.listResources(PackType.SERVER_DATA, namespace, "chatbox/dialogues", ((resourceLocation, inputStreamIoSupplier) -> {
                String path = resourceLocation.getPath();
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(namespace, path.substring("chatbox/dialogues/".length(), path.length() - ".json".length()));
                JsonElement jsonElement = resourceLocationJsonElementMap.get(rl);
                if (jsonElement != null) dialoguesMap.put(rl, jsonElement);
            })));
        });
    }

    public Map<String, List<ChatBoxDialogues>> getDialogues(ResourceLocation resourceLocation) {
        JsonElement jsonElement = this.dialoguesMap.get(resourceLocation);
        if (jsonElement == null) return null;
        JsonElement dialoguesElement = jsonElement.getAsJsonObject().get("dialogues");
        if (dialoguesElement != null) {
            Map<String, List<ChatBoxDialogues>> map = GSON.fromJson(dialoguesElement, new TypeToken<Map<String, List<ChatBoxDialogues>>>() {
            }.getType());
            for (Map.Entry<String, List<ChatBoxDialogues>> entry : map.entrySet()) {
                int index = 0;
                entry.getValue().forEach(chatBoxDialogues -> {
                    chatBoxDialogues.setDefaultValue(resourceLocation, entry.getKey(), index);
                });
            }
            return map;
        }
        return null;
    }
}
