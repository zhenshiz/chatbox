package com.zhenshiz.chatbox.data;

import com.google.gson.*;
//? >= 1.21 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
//?} else {
/*import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.world.level.storage.loot.LootDataManager;
*///?}
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ChatBoxDialoguesLoader extends ChatBoxDataLoader {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    //记录所有的对话（字符串形式）
    public static final Map<ResourceLocation, String> dialoguesMap = new HashMap<>();
    //记录所有的对话（对象形式）
    public static final Map<ResourceLocation, ChatBoxDialogues> parsedDialogues = new HashMap<>();
    //记录对应对话文件里的组名
    public static final Map<ResourceLocation, Set<String>> dialoguesGroupMap = new HashMap<>();

    //套娃，第一个（Map）是文件，第二个是组，第三个是criteria，由于目前的组名下是一个数组，所以只能把判据绑定给同一个json文件里面的第一组对话。解决办法1.花大力气改json格式；2.告诉玩家一个json文件只允许一个组。
    //? >= 1.21 {
    private static final Map<ResourceLocation, Map<String, Map<String, Criterion<?>>>> dialoguesCriteriaMap = new HashMap<>();
    private static final Codec<Map<String, Criterion<?>>> CRITERIA_CODEC = Codec.unboundedMap(Codec.STRING, Criterion.CODEC).validate(map -> map.isEmpty() ? DataResult.error(() -> "Advancement criteria cannot be empty") : DataResult.success(map)); //这个是Advancement类里面的
    //?} else {
    /*private static final Map<ResourceLocation, Map<String, Map<String, Criterion>>> dialoguesCriteriaMap = new HashMap<>();
    private static final LootDataManager lootDataManager = new LootDataManager();
    *///?}
    //记录对话最大触发次数的初始值，用于重设。
    public static final Map<ResourceLocation, Integer> defaultMaxTriggerCount = new HashMap<>();

    public ChatBoxDialoguesLoader() {
        super("chatbox/dialogues");
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, String> map, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        dialoguesMap.clear();
        parsedDialogues.clear();
        dialoguesGroupMap.clear();
        dialoguesCriteriaMap.clear();
        defaultMaxTriggerCount.clear();
        dialoguesMap.putAll(map);
        map.forEach((resourceLocation, str) -> {
            ChatBoxDialogues chatBoxDialogues = GSON.fromJson(str, ChatBoxDialogues.class);
            parsedDialogues.put(resourceLocation, chatBoxDialogues);
            dialoguesGroupMap.put(resourceLocation, chatBoxDialogues.dialogues.keySet());
            defaultMaxTriggerCount.put(resourceLocation, chatBoxDialogues.maxTriggerCount);
            //? < 1.21 {
            /*JsonElement criteriaElement = chatBoxDialogues.criteria;
            if (criteriaElement != null) {
                JsonObject criteriaObject = criteriaElement.getAsJsonObject();
                String group = dialoguesGroupMap.get(resourceLocation).stream().toList().get(0);
                try {
                    Map<String, Criterion> criteria = Criterion.criteriaFromJson(criteriaObject, new DeserializationContext(resourceLocation, lootDataManager));
                    Map<String, Map<String, Criterion>> groupCriteriaMap = new HashMap<>();
                    groupCriteriaMap.put(group, criteria);
                    dialoguesCriteriaMap.put(resourceLocation, groupCriteriaMap);
                } catch (Exception var6x) {
                    ChatBox.LOGGER.error("Parsing error loading dialog {}: {}", resourceLocation, var6x.getMessage());
                }
            }*///?}
        });
    }


    //? >= 1.21
    public static <T extends SimpleCriterionTrigger.SimpleInstance> void triggerDialog(ServerPlayer player, Predicate<T> testTrigger) {
    //? < 1.21
    /*public static <T extends AbstractCriterionTriggerInstance> void triggerDialog(ServerPlayer player, Predicate<T> testTrigger) {*/
        for (var entry : dialoguesCriteriaMap.entrySet()) {
            var rl = entry.getKey();
            var groupWithCriteria = entry.getValue();
            for (var entry1 : groupWithCriteria.entrySet()) {
                var group = entry1.getKey();
                var criteria = entry1.getValue();
                for (var entry2 : criteria.entrySet()) {
                    var criterion = entry2.getValue();
                    //? >= 1.21
                    CriterionTriggerInstance instance = criterion.triggerInstance();
                    //? < 1.21
                    /*CriterionTriggerInstance instance = criterion.getTrigger();*/
                    try {
                        //noinspection unchecked
                        if (testTrigger.test((T) instance)) {
                            //判断玩家的触发次数是否为0，为0则不触发对话
                            int count = ChatBoxCommandUtil.serverGetMaxTriggerCount(player, rl);
                            if (count != 0) {
                                ChatBoxCommandUtil.serverSetMaxTriggerCount(player, rl, count - 1);
                                ChatBoxCommandUtil.serverSkipDialogues(player, rl, group);
                            }
                        }
                    } catch (ClassCastException ignored) {}
                }
            }
        }
    }

    //解析判据必须要用到registryAccess，fabric我目前没想到别的解决办法
    //? >= 1.21 {
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
    }//?}
}
