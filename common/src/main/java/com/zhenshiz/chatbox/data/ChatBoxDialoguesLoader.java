package com.zhenshiz.chatbox.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ChatBoxDialoguesLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    public static final ChatBoxDialoguesLoader INSTANCE = new ChatBoxDialoguesLoader();
    //记录所有的对话文件
    public static final Map<ResourceLocation, String> dialoguesMap = new HashMap<>();
    //记录对应对话文件里的组名
    public static final Map<ResourceLocation, Set<String>> dialoguesGroupMap = new HashMap<>();
    //套娃，第一个（Map）是文件，第二个是组，第三个是criteria，由于目前的组名下是一个数组，所以只能把判据绑定给同一个json文件里面的第一组对话。解决办法1.花大力气改json格式；2.告诉玩家一个json文件只允许一个组。
    private static final Map<ResourceLocation, Map<String, Map<String, Criterion>>> dialoguesCriteriaMap = new HashMap<>();
    //记录对话最大触发次数的初始值，用于重设。
    public static final Map<ResourceLocation, Integer> defaultMaxTriggerCount = new HashMap<>();
    private final LootDataManager lootDataManager = new LootDataManager();

    public ChatBoxDialoguesLoader() {
        super(GSON, "chatbox/dialogues");
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        dialoguesMap.clear();
        resourceLocationJsonElementMap.forEach((resourceLocation, jsonElement) -> dialoguesMap.put(resourceLocation, jsonElement.toString()));
        setDialogues();
    }

    public static <T extends AbstractCriterionTriggerInstance> void triggerDialog(ServerPlayer player, Predicate<T> testTrigger) {
        for (var entry : dialoguesCriteriaMap.entrySet()) {
            var rl = entry.getKey();
            var groupWithCriteria = entry.getValue();
            for (var entry1 : groupWithCriteria.entrySet()) {
                var group = entry1.getKey();
                var criteria = entry1.getValue();
                for (var entry2 : criteria.entrySet()) {
                    var criterion = entry2.getValue();
                    CriterionTriggerInstance instance = criterion.getTrigger();
                    try {
                        //noinspection unchecked
                        T t = (T) instance;
                        if (testTrigger.test(t)) {
                            //判断玩家的触发次数是否为0，为0则不触发对话
                            ChatBoxTriggerCount counts = ChatBox.getTriggerCounts();
                            int count = counts.getPlayerMaxTriggerCount(player, rl);
                            if (count != 0) {
                                counts.setPlayerMaxTriggerCount(player, rl, count - 1);
                                ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.OpenScreen(rl, group, 0));
                            }
                        }
                    } catch (ClassCastException ignored) {}
                }
            }
        }
    }

    private void setDialogues() {
        ChatBoxDialoguesLoader.dialoguesMap.forEach((resourceLocation, str) -> {
            JsonElement jsonElement = GSON.fromJson(str, JsonElement.class);
            if (jsonElement == null) return;
            JsonElement dialoguesElement = jsonElement.getAsJsonObject().get("dialogues");
            if (dialoguesElement != null) {
                ChatBoxDialogues chatBoxDialogues = GSON.fromJson(jsonElement, new TypeToken<ChatBoxDialogues>() {}.getType());
                dialoguesGroupMap.put(resourceLocation, chatBoxDialogues.dialogues.keySet());

                int maxTriggerCount; //仅在服务端加载
                JsonElement maxTriggerCountElement = jsonElement.getAsJsonObject().get("maxTriggerCount");
                if (maxTriggerCountElement != null) {
                    maxTriggerCount = maxTriggerCountElement.getAsInt();
                } else maxTriggerCount = -1;
                defaultMaxTriggerCount.put(resourceLocation, maxTriggerCount);

                //记录判据的jsonElement，用于服务端解析
                JsonElement criteriaElement = jsonElement.getAsJsonObject().get("criteria");
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
                }
            }
        });
    }
}
