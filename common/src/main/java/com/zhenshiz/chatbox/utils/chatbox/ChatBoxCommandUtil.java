package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.common.StrUtil;
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
        LinkedHashMap<Integer, CompoundTag> entityTags = new LinkedHashMap<>();
        for (Entity entity : serverGetChatTargets(player)) {
            try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(entity.problemPath(), ChatBox.LOGGER)) {
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
        toggleTheme(Identifier.parse(theme));
        themeIdentifier = theme;
    }

    public static void serverSkipDialogues(ServerPlayer player, Identifier dialogues, String group, Integer index, List<Entity> targets) {
        ChatBoxCommand.TARGETS_MAP.put(player.getUUID(), targets);
        serverSyncEntityData(player);
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.OpenScreen(dialogues, group, index));
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
        chatBoxScreen.dialogBoxClick();
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

    public static void serverClearChatOption(ServerPlayer player) {
        simplePayloadS2C(player, CLEAR_CHAT_OPTION, "");
    }

    public static void clientClearChatOption() {
        chatBoxScreen.chatOptions.clear();
    }

    // 服务端并不能获取当前客户端的选项信息，故不提供服务端解锁以及隐藏选项的方法
    public static void clientUnlockChatOption(int index) {
        List<ChatOption> options = chatBoxScreen.chatOptions;
        if (index < 0 || index >= options.size()) return;
        options.get(index).setIsLock(false);
    }

    public static void clientHideChatOption(int index) {
        List<ChatOption> options = chatBoxScreen.chatOptions;
        if (index < 0 || index >= options.size()) return;
        options.get(index).renderIndex = -1;
    }

    public static void addPlaceholderResolver(String key, Function<Entity, String> resolver) {
        addPropertyResolver(key, resolver);
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
