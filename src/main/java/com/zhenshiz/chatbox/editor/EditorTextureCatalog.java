package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import java.util.TreeMap;

/** 列出已启用的本地资源包贴图，不枚举模组内置资源。 */
final class EditorTextureCatalog {
    private EditorTextureCatalog() { }

    /**
     * 列出已启用的压缩包或目录资源包中实际生效的 PNG 和 JPEG 贴图。
     *
     * <p>只读取资源索引，图片内容按需获取。
     * 忽略被更高优先级资源包覆盖或被资源过滤器屏蔽的贴图。
     *
     * @param manager 包含客户端当前已加载资源包的资源管理器
     * @return 排序后的贴图选项，包含各贴图实际来源的资源包 ID
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
