package com.zhenshiz.chatbox.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.event.ChatBoxSettingLoader;
import com.zhenshiz.chatbox.network.s2c.ClientChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ChatBoxDialoguesLoader extends ChatBoxDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    //记录所有的对话文件
    public static final Map<ResourceLocation, String> dialoguesMap = new HashMap<>();
    //记录所有的对话（对象形式）
    public static final Map<ResourceLocation, ChatBoxDialogues> parsedDialogues = new HashMap<>();
    //记录对应对话文件里的组名
    public static final Map<ResourceLocation, Set<String>> dialoguesGroupMap = new HashMap<>();

    //套娃，第一个（Map）是文件，第二个是组，第三个是criteria，由于目前的组名下是一个数组，所以只能把判据绑定给同一个json文件里面的第一组对话。解决办法1.花大力气改json格式；2.告诉玩家一个json文件只允许一个组。
    private static final Map<ResourceLocation, Map<String, Map<String, Criterion<?>>>> dialoguesCriteriaMap = new HashMap<>();
    private static final Codec<Map<String, Criterion<?>>> CRITERIA_CODEC = Codec.unboundedMap(Codec.STRING, Criterion.CODEC).validate(map -> map.isEmpty() ? DataResult.error(() -> "Advancement criteria cannot be empty") : DataResult.success(map)); //这个是Advancement类里面的

    public ChatBoxDialoguesLoader() {
        super("chatbox/dialogues");
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, String> map, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        dialoguesMap.clear();
        parsedDialogues.clear();
        dialoguesGroupMap.clear();
        dialoguesCriteriaMap.clear();
        dialoguesMap.putAll(map);
        map.forEach((resourceLocation, str) -> {
            ChatBoxDialogues chatBoxDialogues = GSON.fromJson(str, ChatBoxDialogues.class);
            parsedDialogues.put(resourceLocation, chatBoxDialogues);
            dialoguesGroupMap.put(resourceLocation, chatBoxDialogues.dialogues.keySet());
        });

        //给所有玩家发包
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(serverPlayer -> serverPlayer.connection.send(new ClientChatBoxPayload.ChatBoxDataToClient("dialogues", ChatBoxSettingLoader.cutString(dialoguesMap))));
            loadCriteria(ServerLifecycleHooks.getCurrentServer());
        }
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
                        // noinspection unchecked
                        if (testTrigger.test((T) instance)) ChatBoxCommandUtil.serverSkipDialogues(player, rl, group);
                    } catch (ClassCastException ignored) {}
                }
            }
        }
    }

    public static void loadCriteria(MinecraftServer server) {
        for (var entry : parsedDialogues.entrySet()) {
            ResourceLocation rl = entry.getKey();
            JsonElement criteriaElement = entry.getValue().criteria;
            if (criteriaElement == null) continue;

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
}
