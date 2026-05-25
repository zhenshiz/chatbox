package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import com.zhenshiz.chatbox.utils.mvel.MVELUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.zhenshiz.chatbox.network.SimplePayload.*;
import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.*;

public class ChatBoxCommandUtil {

    public static void serverSyncEntityData(ServerPlayer player) {
        var entityTags = new LinkedHashMap<Integer, CompoundTag>();
        for (Entity entity : serverGetChatTargets(player)) {
            try (var scopedCollector = new ProblemReporter.ScopedCollector(entity.problemPath(), ChatBox.LOGGER)) {
                TagValueOutput tagValueOutput = TagValueOutput.createWithContext(scopedCollector, entity.registryAccess());
                entity.saveWithoutId(tagValueOutput);
                entityTags.put(entity.getId(), tagValueOutput.buildResult());
            }
        }
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.SyncEntityData(entityTags));
    }

    public static void serverToggleTheme(ServerPlayer player, Identifier theme) {
        simplePayloadS2C(player, SET_THEME, theme.toString());
    }

    public static void clientToggleTheme(String theme) {
        toggleTheme(theme);
    }

    public static void serverSkipDialogues(ServerPlayer player, Identifier dialogues, String group, Integer index, List<Entity> targets) {
        ChatBoxCommand.TARGETS_MAP.put(player.getUUID(), targets);
        serverSyncEntityData(player);
        simplePayloadS2C(player, SKIP_CHAT_S2C, StrUtil.merge(dialogues, group, index));
    }

    public static void serverSkipDialogues(ServerPlayer player, Identifier dialogues, String group, Entity... targets) {
        serverSkipDialogues(player, dialogues, group, 0, List.of(targets));
    }

    public static void clientSkipDialogues(Identifier dialogues, String group, Integer index) {
        skipDialogues(dialogues, group, index);
    }

    public static void clientSkipDialogues(Identifier dialogues, String group) {
        clientSkipDialogues(dialogues, group, 0);
    }

    public static List<Entity> serverGetChatTargets(ServerPlayer player) {
        return ChatBoxCommand.TARGETS_MAP.getOrDefault(player.getUUID(), List.of());
    }

    public static void serverOpenChatBox(ServerPlayer player) {
        simplePayloadS2C(player, OPEN_DIALOG, "");
    }

    public static void clientOpenChatBox() {
        if (dialoguesIdentifier != null && group != null && index != null) {
            skipDialogues(dialoguesIdentifier, group, index);
        }
    }

    public static void serverNextDialogue(ServerPlayer player) {
        simplePayloadS2C(player, NEXT_DIALOGUE, "");
    }

    public static void clientNextDialogue() {
        if (chatBoxScreen.shouldGotoNext()) skipDialogues(dialoguesIdentifier, group, index + 1);
    }

    public static void serverAutoPlay(ServerPlayer player, boolean autoPlay) {
        simplePayloadS2C(player, AUTO_PLAY, String.valueOf(autoPlay));
    }

    public static void clientAutoPlay(boolean autoPlay) {
        chatBoxScreen.autoPlay = autoPlay;
    }

    public static void serverSetIsScreen(ServerPlayer player, boolean isScreen) {
        simplePayloadS2C(player, SET_IS_SCREEN, String.valueOf(isScreen));
    }

    public static void clientSetIsScreen(boolean isScreen) {
        ChatBoxUtil.isScreen = isScreen;
    }

    public static int serverGetMaxTriggerCount(ServerPlayer player, Identifier dialogIdentifier) {
        return ChatBox.getTriggerCounts().getPlayerMaxTriggerCount(player, dialogIdentifier);
    }

    public static void serverSetMaxTriggerCount(ServerPlayer player, Identifier dialogIdentifier, int count) {
        ChatBox.getTriggerCounts().setPlayerMaxTriggerCount(player, dialogIdentifier, count);
    }

    public static void serverResetMaxTriggerCount(ServerPlayer player) {
        ChatBox.getTriggerCounts().resetPlayerMaxTriggerCount(player);
    }

    public static void serverSetDialogBox(ServerPlayer player, String name, String text) {
        simplePayloadS2C(player, SET_DIALOG_BOX, StrUtil.merge(name, text));
    }

    public static void clientSetDialogBox(String name, String text) {
        chatBoxScreen.dialogBox.setName(name).setText(text).setAllOver(false);
        var historicalInfo = historicalDialogue.historicalDialogue.historicalInfos.getLast();
        historicalInfo.name = name;
        historicalInfo.text = text;
    }

    public static void serverAddChatOption(ServerPlayer player, String text, String next, String tip, String clickType, String clickValue) {
        simplePayloadS2C(player, ADD_CHAT_OPTION, StrUtil.merge(text, next, tip, clickType, clickValue));
    }

    public static void clientAddChatOption(String text, String next, String tip, String clickType, String clickValue) {
        ChatOption option = chatBoxTheme.option.newOption()
                .setOptionChat(text).setNext(next).setOptionTooltip(tip).setClickEvent(clickType, clickValue);
        chatBoxScreen.addChatOptions(option);
    }

    public static void serverSetChatOption(ServerPlayer player, int index, String text, String tip, Boolean lock, Boolean hide) {
        simplePayloadS2C(player, SET_CHAT_OPTION, StrUtil.merge(index, text, tip, lock, hide));
    }

    public static void clientSetChatOption(int index, String text, String tip, Boolean lock, Boolean hide) {
        var options = chatBoxScreen.chatOptions;
        if (index < 0 || index >= options.size()) return;
        var option = options.get(index);
        option.setOptionChat(text).setOptionTooltip(tip).setIsLock(lock).hideOption(hide);
    }

    public static void serverClearChatOption(ServerPlayer player) {
        simplePayloadS2C(player, CLEAR_CHAT_OPTION, "");
    }

    public static void clientClearChatOption() {
        chatBoxScreen.chatOptions.clear();
    }

    public static void addMvelMethod(String name, MVELUtil.DynamicMethod handler) {
        MVELUtil.registerMethod(name, handler);
    }

    public static void addMvelProperty(String name, MVELUtil.DynamicProperty handler) {
        MVELUtil.registerProperty(name, handler);
    }

    public static void addPlaceholderResolver(String key, Function<Entity, Object> resolver) {
        MVELUtil.addPropertyResolver(key, resolver);
    }

    public static String parseTargetPlaceholders(ServerPlayer player, String input) {
        return MVELUtil.parseTargetPlaceholders(player, input);
    }

    public static void simplePayloadS2C(ServerPlayer player, String name, String value) {
        ChatBox.PLATFORM.sendToClient(player, new SimplePayload(name, value));
    }

    public static void addSimpleHandlerS2C(String name, Consumer<String> handler) {
        SimplePayload.addHandlerS2C(name, handler);
    }

    public static void simplePayloadC2S(String name, String value) {
        ChatBox.PLATFORM.sendToServer(new SimplePayload(name, value));
    }

    public static void addSimpleHandlerC2S(String name, BiConsumer<ServerPlayer, String> handler) {
        SimplePayload.addHandlerC2S(name, handler);
    }

}
