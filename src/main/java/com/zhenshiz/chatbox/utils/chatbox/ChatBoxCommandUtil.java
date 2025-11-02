package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.api.ChatOptionClickEvent;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.network.c2s.ServerChatBoxPayload;
import com.zhenshiz.chatbox.network.s2c.ClientChatBoxPayload;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        player.connection.send(new ClientChatBoxPayload.SyncEntityData(entityTags));
    }

    @Info("服务端切换对话框主题样式")
    public static void serverToggleTheme(ServerPlayer player, ResourceLocation theme) {
        simplePayloadS2C(player, SET_THEME, theme.toString());
    }

    @Info("客户端切换对话框主题样式")
    public static void clientToggleTheme(String theme) {
        toggleTheme(ResourceLocation.parse(theme));
        themeResourceLocation = theme;
    }

    @Info("服务端跳转对话")
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index, List<Entity> targets) {
        ChatBoxCommand.TARGETS_MAP.put(player.getUUID(), targets);
        serverSyncEntityData(player);
        player.connection.send(new ClientChatBoxPayload.OpenScreenPayload(dialogues, group, index, entityListToString(targets)));
    }

    @HideFromJS
    @Info("服务端跳转对话")
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Entity... targets) {
        serverSkipDialogues(player, dialogues, group, 0, List.of(targets));
    }

    @Info("客户端跳转对话")
    public static void clientSkipDialogues(ResourceLocation dialogues, String group, Integer index) {
        if (testMaxTriggerCount(dialogues)) skipDialogues(dialogues, group, index);
    }

    @Info("客户端跳转对话，默认第一句话")
    public static void clientSkipDialogues(ResourceLocation dialogues, String group) {
        clientSkipDialogues(dialogues, group, 0);
    }

    @Info("获取当前对话的实体")
    public static List<Entity> serverGetChatTargets(ServerPlayer player) {
        return ChatBoxCommand.TARGETS_MAP.getOrDefault(player.getUUID(), List.of());
    }

    @Info("服务端打开对话框，无视最大访问次数")
    public static void serverOpenChatBox(ServerPlayer player) {
        simplePayloadS2C(player, OPEN_DIALOG, "");
    }

    @Info("客户端打开对话框，无视最大访问次数")
    public static void clientOpenChatBox() {
        if (Minecraft.getInstance().player != null && dialoguesResourceLocation != null && group != null && index != null) {
            skipDialogues(dialoguesResourceLocation, group, index);
        }
    }

    @Info("服务端跳转下一条对话")
    public static void serverNextDialogue(ServerPlayer player) {
        simplePayloadS2C(player, NEXT_DIALOGUE, "");
    }

    @Info("客户端跳转下一条对话")
    public static void clientNextDialogue() {
        chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
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

    @Info("服务端设置最大访问次数，自带同步")
    public static void serverSetMaxTriggerCount(ServerPlayer player, ResourceLocation dialogResourceLocation, int count) {
        if (player != null) {
            ChatBoxTriggerCount.MaxTriggerCount maxTriggerCount = player.getData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT);
            String resourceLocation = dialogResourceLocation.toString();
            Map<String, Integer> triggerCounts = maxTriggerCount.getTriggerCounts();
            Map<String, Integer> newTriggerCounts = new HashMap<>(triggerCounts);
            newTriggerCounts.put(resourceLocation, count);
            maxTriggerCount.setTriggerCounts(newTriggerCounts);
            player.setData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT, maxTriggerCount);
        }
    }

    @Info("客户端设置最大访问次数，自带同步")
    public static void clientSetMaxTriggerCount(ResourceLocation dialogResourceLocation, int count) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.send(new ServerChatBoxPayload.SetMaxTriggerCount(dialogResourceLocation, count));
        }
    }

    @Info("服务端重置访问次数，自带同步")
    public static void serverResetMaxTriggerCount(ServerPlayer player) {
        if (player != null) {
            player.setData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT, new ChatBoxTriggerCount.MaxTriggerCount());
        }
    }

    @Info("客户端重置访问次数，自带同步")
    public static void clientResetMaxTriggerCount() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.send(new ServerChatBoxPayload.ResetMaxTriggerCount());
        }
    }

    @Info("服务端设置对话框")
    public static void serverSetDialogBox(ServerPlayer player, String name, String text) {
        simplePayloadS2C(player, SET_DIALOG_BOX, StrUtil.merge(name, text));
    }

    @Info("客户端设置对话框")
    public static void clientSetDialogBox(String name, String text) {
        chatBoxScreen.dialogBox.setName(name, true).setText(text, true).resetTickCount().setAllOver(false);
    }

    @Info("服务端添加选项")
    public static void serverAddChatOption(ServerPlayer player, String text, String next, String tip, String clickType, String clickValue) {
        simplePayloadS2C(player, ADD_CHAT_OPTION, StrUtil.merge(text, next, tip, clickType, clickValue));
    }

    @Info("客户端添加选项")
    public static void clientAddChatOption(String text, String next, String tip, String clickType, String clickValue) {
        ChatOption option = new ChatOption().setOptionChat(text, true).setNext(next).setOptionTooltip(tip, true).setClickEvent(clickType, clickValue);
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

    // 服务端并不能获取当前客户端的选项信息，故不提供服务端解锁以及隐藏选项的方法
    @Info("客户端解锁选项")
    public static void clientUnlockChatOption(int index) {
        List<ChatOption> options = chatBoxScreen.chatOptions;
        if (index < 0 || index >= options.size()) return;
        options.get(index).setIsLock(false);
    }

    @Info("客户端隐藏选项")
    public static void clientHideChatOption(int index) {
        List<ChatOption> options = chatBoxScreen.chatOptions;
        if (index < 0 || index >= options.size()) return;
        options.get(index).renderIndex = -1;
    }

    @Info("注册一个选项点击事件，可以在服务端任意位置使用")
    public static void registerClickEvent(String type, Consumer<String> executeOnClient, Boolean shouldExecuteOnServer, BiConsumer<ServerPlayer, String> executeOnServer) {
        ChatOptionClickEvent.registerClickEvent(type, executeOnClient, shouldExecuteOnServer, executeOnServer);
    }

    @Info("添加一个占位符属性解析器，在客户端任意位置使用")
    public static void addPlaceholderResolver(String key, Function<Entity, String> resolver) {
        addPropertyResolver(key, resolver);
    }

    private static boolean testMaxTriggerCount(ResourceLocation dialogues) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;
        //判断该对话是否有触发的次数限制
        ChatBoxDialogues chatBoxDialogues = dialoguesMap.get(dialogues);
        ChatBoxTriggerCount.MaxTriggerCount maxTriggerCount = minecraft.player.getData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT);
        String resourceLocation = dialogues.toString();
        Map<String, Integer> triggerCounts = maxTriggerCount.getTriggerCounts();
        Integer count = triggerCounts.get(resourceLocation);
        if (count == null) {
            minecraft.player.connection.send(new ServerChatBoxPayload.SetMaxTriggerCount(dialogues, chatBoxDialogues.maxTriggerCount - 1));
        } else {
            if (count == 0) return false;
            minecraft.player.connection.send(new ServerChatBoxPayload.SetMaxTriggerCount(dialogues, count - 1));
        }
        String theme = chatBoxDialogues.theme;
        if (theme != null && !theme.equals(themeResourceLocation)) clientToggleTheme(theme);
        if (chatBoxDialogues.isScreen != null) clientSetIsScreen(chatBoxDialogues.isScreen);
        return true;
    }
}
