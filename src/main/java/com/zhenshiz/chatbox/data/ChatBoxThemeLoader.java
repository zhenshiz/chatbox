package com.zhenshiz.chatbox.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ChatBoxThemeLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON =
            (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    public static final ChatBoxThemeLoader INSTANCE = new ChatBoxThemeLoader();
    public final Map<ResourceLocation, JsonElement> themeMap = new HashMap<>();

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
                if (jsonElement != null) themeMap.put(rl, jsonElement);
            })));
        });
    }

    public ChatBoxTheme getTheme(ResourceLocation resourceLocation) {
        JsonElement jsonElement = this.themeMap.get(resourceLocation);
        if (jsonElement == null) return null;
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        JsonElement portraitElement = jsonObject.get("portrait");
        JsonElement chatOptionElement = jsonObject.get("option");
        JsonElement dialogBoxElement = jsonObject.get("dialogBox");
        JsonElement logButtonElement = jsonObject.get("logButton");
        Map<String, ChatBoxTheme.Portrait> portrait = new HashMap<>();
        ChatBoxTheme.Option option = new ChatBoxTheme.Option();
        ChatBoxTheme.DialogBox dialogBox = new ChatBoxTheme.DialogBox();
        ChatBoxTheme.LogButton logButton = new ChatBoxTheme.LogButton();

        if (portraitElement != null) {
            portrait = GSON.fromJson(portraitElement, new TypeToken<Map<String, ChatBoxTheme.Portrait>>() {
            }.getType());
        }
        if (chatOptionElement != null) {
            option = GSON.fromJson(chatOptionElement, ChatBoxTheme.Option.class);
        }
        if (dialogBoxElement != null) {
            dialogBox = GSON.fromJson(dialogBoxElement, ChatBoxTheme.DialogBox.class);
        }
        if (logButtonElement != null) {
            logButton = GSON.fromJson(logButtonElement, ChatBoxTheme.LogButton.class);
        }

        return new ChatBoxTheme(portrait, option, dialogBox, logButton).setDefaultValue();
    }
}
