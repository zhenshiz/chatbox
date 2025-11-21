package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.api.EventExecutor;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.zhenshiz.chatbox.network.SimplePayload.*;
import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.*;

public class ChatBoxCommandUtil {

    @HideFromJS
    public static void serverSyncEntityData(ServerPlayer player) {
        LinkedHashMap<Integer, CompoundTag> entityTags = new LinkedHashMap<>();
        for (Entity entity : serverGetChatTargets(player)) {
            CompoundTag tag = new CompoundTag();
            entity.saveWithoutId(tag);
            entityTags.put(entity.getId(), tag);
        }
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.SyncEntityData(entityTags));
    }

    @Info("服务端切换对话框主题样式")
    public static void serverToggleTheme(ServerPlayer player, ResourceLocation theme) {
        simplePayloadS2C(player, SET_THEME, theme.toString());
    }

    @Info("客户端切换对话框主题样式")
    public static void clientToggleTheme(String theme) {
        toggleTheme(new ResourceLocation(theme));
        themeResourceLocation = theme;
    }

    @Info("服务端跳转对话")
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group) {
        serverSkipDialogues(player, dialogues, group, 0, List.of(player));
    }

    @Info("服务端跳转对话")
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index) {
        serverSkipDialogues(player, dialogues, group, index, List.of(player));
    }

    @Info("服务端跳转对话")
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, List<Entity> targets) {
        serverSkipDialogues(player, dialogues, group, 0, targets);
    }

    @Info("服务端跳转对话")
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index, List<Entity> targets) {
        ChatBoxCommand.TARGETS_MAP.put(player.getUUID(), targets);
        serverSyncEntityData(player);
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.OpenScreen(dialogues, group, index));
    }

    @Info("客户端跳转对话")
    public static void clientSkipDialogues(ResourceLocation dialogues, String group, Integer index) {
        skipDialogues(dialogues, group, index);
    }

    @Info("客户端跳转对话，默认第一句话")
    public static void clientSkipDialogues(ResourceLocation dialogues, String group) {
        clientSkipDialogues(dialogues, group, 0);
    }

    @Info("获取当前对话的实体")
    public static List<Entity> serverGetChatTargets(ServerPlayer player) {
        return ChatBoxCommand.TARGETS_MAP.getOrDefault(player.getUUID(), List.of());
    }

    @Info("服务端打开最近打开的对话框，无视最大访问次数")
    public static void serverOpenChatBox(ServerPlayer player) {
        simplePayloadS2C(player, OPEN_DIALOG, "");
    }

    @Info("客户端打开最近打开的对话框，无视最大访问次数")
    public static void clientOpenChatBox() {
        if (dialoguesResourceLocation != null && group != null && index != null) {
            skipDialogues(dialoguesResourceLocation, group, index);
        }
    }

    @Info("服务端跳转下一条对话")
    public static void serverNextDialogue(ServerPlayer player) {
        simplePayloadS2C(player, NEXT_DIALOGUE, "");
    }

    @Info("客户端跳转下一条对话")
    public static void clientNextDialogue() {
        chatBoxScreen.dialogBoxClick();
    }

    @Info("服务端开关自动对话")
    public static void serverAutoPlay(ServerPlayer player, boolean autoPlay) {
        simplePayloadS2C(player, AUTO_PLAY, String.valueOf(autoPlay));
    }

    @Info("客户端开关自动对话")
    public static void clientAutoPlay(boolean autoPlay) {
        chatBoxScreen.autoPlay = autoPlay;
    }

    @Info("服务端设置对话框是否为屏幕")
    public static void serverSetIsScreen(ServerPlayer player, boolean isScreen) {
        simplePayloadS2C(player, SET_IS_SCREEN, String.valueOf(isScreen));
    }

    @Info("客户端切换对话框是否为屏幕")
    public static void clientSetIsScreen(boolean isScreen) {
        ChatBoxUtil.isScreen = isScreen;
    }

    @Info("获取最大访问次数")
    public static int serverGetMaxTriggerCount(ServerPlayer player, ResourceLocation dialogResourceLocation) {
        return ChatBox.getTriggerCounts().getPlayerMaxTriggerCount(player, dialogResourceLocation);
    }

    @Info("设置最大访问次数")
    public static void serverSetMaxTriggerCount(ServerPlayer player, ResourceLocation dialogResourceLocation, int count) {
        ChatBox.getTriggerCounts().setPlayerMaxTriggerCount(player, dialogResourceLocation, count);
    }

    @Info("重置最大访问次数")
    public static void serverResetMaxTriggerCount(ServerPlayer player) {
        ChatBox.getTriggerCounts().resetPlayerMaxTriggerCount(player);
    }

    @Info("服务端设置对话框")
    public static void serverSetDialogBox(ServerPlayer player, String name, String text) {
        simplePayloadS2C(player, SET_DIALOG_BOX, StrUtil.merge(name, text));
    }

    @Info("客户端设置对话框")
    public static void clientSetDialogBox(String name, String text) {
        chatBoxScreen.dialogBox.setName(name).setText(text).resetTickCount().setAllOver(false);
        var historicalInfos = historicalDialogue.historicalDialogue.historicalInfos;
        historicalInfos.get(historicalInfos.size() - 1).setName(name).setText(text);
    }

    @Info("服务端添加选项")
    public static void serverAddChatOption(ServerPlayer player, String text, String next, String tip, String clickType, String clickValue) {
        simplePayloadS2C(player, ADD_CHAT_OPTION, StrUtil.merge(text, next, tip, clickType, clickValue));
    }

    @Info("客户端添加选项")
    public static void clientAddChatOption(String text, String next, String tip, String clickType, String clickValue) {
        ChatOption option = new ChatOption().setOptionChat(text).setNext(next).setOptionTooltip(tip).setClickEvent(clickType, clickValue);
        chatBoxTheme.option.setChatOptionTheme(option);
        chatBoxScreen.addChatOptions(option);
    }

    @Info("服务端清除选项")
    public static void serverClearChatOption(ServerPlayer player) {
        simplePayloadS2C(player, CLEAR_CHAT_OPTION, "");
    }

    @Info("客户端清除选项")
    public static void clientClearChatOption() {
        chatBoxScreen.chatOptions.clear();
    }

    @Info("客户端解锁选项，服务端并不能获取客户端的选项信息，故不提供服务端的方法")
    public static void clientUnlockChatOption(int index) {
        List<ChatOption> options = chatBoxScreen.chatOptions;
        if (index < 0 || index >= options.size()) return;
        options.get(index).setIsLock(false);
    }

    @Info("客户端隐藏选项，服务端并不能获取客户端的选项信息，故不提供服务端的方法")
    public static void clientHideChatOption(int index) {
        List<ChatOption> options = chatBoxScreen.chatOptions;
        if (index < 0 || index >= options.size()) return;
        options.get(index).renderIndex = -1;
    }

    @Info("注册一个组件事件，可以在服务端任意位置使用")
    public static void registerComponentEvent(String type, BiConsumer<AbstractComponent<?>, String> executeOnClient, boolean shouldExecuteOnServer, BiConsumer<ServerPlayer, String> executeOnServer) {
        EventExecutor.registerEvent(type, executeOnClient, () -> shouldExecuteOnServer, executeOnServer);
    }

    @Info("添加一个占位符属性解析器，在客户端任意位置使用")
    public static void addPlaceholderResolver(String key, Function<Entity, String> resolver) {
        addPropertyResolver(key, resolver);
    }

    @Info("服务端发送一个简单数据包到客户端")
    public static void simplePayloadS2C(ServerPlayer player, String name, String value) {
        ChatBox.PLATFORM.sendToClient(player, new SimplePayload(name, value));
    }

    @Info("添加一个简单数据包的客户端处理方法")
    public static void addSimpleHandlerS2C(String name, Consumer<String> handler) {
        SimplePayload.addHandlerS2C(name, handler);
    }

    @Info("客户端发送一个简单数据包到服务端")
    public static void simplePayloadC2S(String name, String value) {
        ChatBox.PLATFORM.sendToServer(new SimplePayload(name, value));
    }

    @Info("添加一个简单数据包的服务端处理方法")
    public static void addSimpleHandlerC2S(String name, BiConsumer<ServerPlayer, String> handler) {
        SimplePayload.addHandlerC2S(name, handler);
    }

}
