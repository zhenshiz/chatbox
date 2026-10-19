package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import java.util.TreeMap;

/** Lists textures from enabled local resource packs without enumerating built-in mod assets. */
final class EditorTextureCatalog {
    private EditorTextureCatalog() { }

    /**
     * Lists effective PNG and JPEG textures supplied by enabled file or directory resource packs.
     *
     * <p>Only pack indexes are read; image contents remain available through on-demand requests.
     * Resources hidden by higher-priority packs or resource filters are omitted.
     *
     * @param manager the resource manager containing the client's currently loaded packs
     * @return the sorted texture options, including each effective source pack ID
     */
    static JsonArray collect(ResourceManager manager) {
        Map<ResourceLocation, String> textures = new TreeMap<>();
        manager.listPacks()
                .filter(pack -> pack.location().source() == PackSource.DEFAULT && pack.packId().startsWith("file/"))
                .forEach(pack -> {
                    for (String namespace : pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
                        pack.listResources(PackType.CLIENT_RESOURCES, namespace, "textures", (id, input) -> {
                            String path = id.getPath();
                            if (!path.endsWith(".png") && !path.endsWith(".jpg") && !path.endsWith(".jpeg")) return;
                            manager.getResource(id).filter(resource -> resource.sourcePackId().equals(pack.packId()))
                                    .ifPresent(resource -> textures.put(id, resource.sourcePackId()));
                        });
                    }
                });
        JsonArray options = new JsonArray();
        textures.forEach((id, source) -> {
            JsonObject option = new JsonObject();
            option.addProperty("label", id.toString());
            option.addProperty("value", id.toString());
            option.addProperty("source", source);
            options.add(option);
        });
        return options;
    }
}
