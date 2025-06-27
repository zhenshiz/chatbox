package com.zhenshiz.chatbox.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ChatBoxDialoguesLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
    private static final Gson GSON =
            (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    public static final ChatBoxDialoguesLoader INSTANCE = new ChatBoxDialoguesLoader();
    //记录所有的对话文件
    public final Map<ResourceLocation, String> dialoguesMap = new HashMap<>();
    //记录对应对话文件里的组名
    public static final Map<ResourceLocation, Set<String>> dialoguesGroupMap = new HashMap<>();

    //这一堆东西看的我自己都头大
    private static final Map<ResourceLocation, JsonElement> criteriaElements = new HashMap<>();
    //套娃，第一个（Map）是文件，第二个是组，第三个是criteria，由于目前的组名下是一个数组，所以只能把判据绑定给同一个json文件里面的第一组对话。解决办法1.花大力气改json格式；2.告诉玩家一个json文件只允许一个组。
    private static final Map<ResourceLocation, Map<String, Map<String, Criterion<?>>>> dialoguesCriteriaMap = new HashMap<>();
    private static final Codec<Map<String, Criterion<?>>> CRITERIA_CODEC = Codec.unboundedMap(Codec.STRING, Criterion.CODEC).validate(map -> map.isEmpty() ? DataResult.error(() -> "Advancement criteria cannot be empty") : DataResult.success(map)); //这个是Advancement类里面的
    //记录对话最大触发次数的初始值，用于重设。
    public static final Map<ResourceLocation, Integer> defaultMaxTriggerCount = new HashMap<>();

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
        if (ChatBox.server != null) {
            ChatBox.server.getPlayerList().getPlayers().forEach(serverPlayer -> ServerPlayNetworking.send(serverPlayer, new ChatBoxPayload.AllChatBoxDialoguesToClient(dialoguesMap)));
        }

        setDialogues(dialoguesMap);
    }

    public static <T extends SimpleCriterionTrigger.SimpleInstance> void triggerDialog(ServerPlayer player, Predicate<T> testTrigger) {
        for (var entry : dialoguesCriteriaMap.entrySet()) {
            var rl = entry.getKey();
            var groupWithCriteria = entry.getValue();
            for (var entry1 : groupWithCriteria.entrySet()) {
                var group = entry1.getKey();
                var criteria = entry1.getValue();
                for (var entry2 : criteria.entrySet()) {
                    var criterion = entry2.getValue();
                    CriterionTriggerInstance instance = criterion.triggerInstance();
                    try {
                        T t = (T) instance;
                        if (testTrigger.test(t)) {
                            //判断玩家的触发次数是否为0，为0则不触发对话
                            ChatBoxTriggerCount counts = ChatBox.getTriggerCounts();
                            int count = counts.getPlayerMaxTriggerCount(player, rl);
                            if (count != 0) {
                                counts.setPlayerMaxTriggerCount(player, rl, count - 1);
                                ServerPlayNetworking.send(player, new ChatBoxPayload.OpenScreenPayload(rl, group, 0));
                            }
                        }
                    } catch (ClassCastException e) {
                        continue;
                    }
                }
            }
        }
    }

    private static void setDialogues(Map<ResourceLocation, String> map) {
        map.forEach((resourceLocation, str) -> {
            JsonElement jsonElement = GSON.fromJson(str, JsonElement.class);
            if (jsonElement == null) return;
            JsonElement dialoguesElement = jsonElement.getAsJsonObject().get("dialogues");
            if (dialoguesElement != null) {
                ChatBoxDialogues chatBoxDialogues = GSON.fromJson(jsonElement, new com.google.common.reflect.TypeToken<ChatBoxDialogues>() {
                }.getType());
                dialoguesGroupMap.put(resourceLocation, chatBoxDialogues.dialogues.keySet());

                int maxTriggerCount; //仅在服务端加载
                JsonElement maxTriggerCountElement = jsonElement.getAsJsonObject().get("maxTriggerCount");
                if (maxTriggerCountElement != null) {
                    maxTriggerCount = maxTriggerCountElement.getAsInt();
                } else maxTriggerCount = -1;
                defaultMaxTriggerCount.put(resourceLocation, maxTriggerCount);

                //记录判据的jsonElement，用于服务端解析
                JsonElement criteria = jsonElement.getAsJsonObject().get("criteria");
                if (criteria != null) criteriaElements.put(resourceLocation, criteria);
            }
        });
    }

    //解析判据必须要用到registryAccess，fabric我目前没想到别的解决办法
    public static void loadCriteria(MinecraftServer server) {
        for (var entry : criteriaElements.entrySet()) {
            ResourceLocation rl = entry.getKey();
            JsonElement criteriaElement = entry.getValue();
            String group = dialoguesGroupMap.get(rl).stream().toList().getFirst();
            RegistryOps<JsonElement> registryOps = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            try {
                Map<String, Criterion<?>> criteria = CRITERIA_CODEC.parse(registryOps, criteriaElement).getOrThrow(JsonParseException::new);
                Map<String, Map<String, Criterion<?>>> groupCriteriaMap = new HashMap<>();
                groupCriteriaMap.put(group, criteria);
                dialoguesCriteriaMap.put(rl, groupCriteriaMap);
            } catch (Exception var6x) {
                ChatBox.LOGGER.error("Parsing error loading dialog {}: {}", rl, var6x.getMessage());
            }
        }
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(ChatBox.MOD_ID, "chatbox/dialogues");
    }
}
