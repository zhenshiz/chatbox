package com.zhenshiz.chatbox.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatBoxDialoguesLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON =
            (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    public static final ChatBoxDialoguesLoader INSTANCE = new ChatBoxDialoguesLoader();
    public final Map<ResourceLocation, String> dialoguesMap = new HashMap<>();

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
                if (jsonElement != null) dialoguesMap.put(rl, jsonElement.toString());
            })));
        });

        //给所有玩家发包
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(serverPlayer -> serverPlayer.connection.send(new ChatBoxPayload.AllChatBoxDialoguesToClient(dialoguesMap)));
        }
    }
}
