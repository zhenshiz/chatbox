package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.api.EventExecutor;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import com.zhenshiz.chatbox.utils.mvel.MVELUtil;
//? forge {
/*import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.util.HideFromJS;
*///?}
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

    //? forge
    /*@HideFromJS*/
    public static void serverSyncEntityData(ServerPlayer player) {
        LinkedHashMap<Integer, CompoundTag> entityTags = new LinkedHashMap<>();
        for (Entity entity : serverGetChatTargets(player)) {
            CompoundTag tag = new CompoundTag();
            entity.saveWithoutId(tag);
            entityTags.put(entity.getId(), tag);
        }
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.SyncEntityData(entityTags));
    }

    //? forge
    /*@Info("服务端切换对话框主题样式")*/
    public static void serverToggleTheme(ServerPlayer player, ResourceLocation theme) {
        simplePayloadS2C(player, SET_THEME, theme.toString());
    }

    //? forge
    /*@Info("客户端切换对话框主题样式")*/
    public static void clientToggleTheme(String theme) {
        toggleTheme(theme);
    }

    //? forge
    /*@Info("服务端跳转对话")*/
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index, List<Entity> targets) {
        ChatBoxCommand.TARGETS_MAP.put(player.getUUID(), targets);
        serverSyncEntityData(player);
        simplePayloadS2C(player, SKIP_CHAT_S2C, StrUtil.merge(dialogues, group, index));
    }

    //? forge
    /*@Info("服务端跳转对话")*/
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group) {
        serverSkipDialogues(player, dialogues, group, 0, List.of(player));
    }

    //? forge
    /*@Info("服务端跳转对话")*/
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index) {
        serverSkipDialogues(player, dialogues, group, index, List.of(player));
    }

    //? forge
    /*@Info("服务端跳转对话")*/
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, List<Entity> targets) {
        serverSkipDialogues(player, dialogues, group, 0, targets);
    }

    //? forge
    /*@Info("客户端跳转对话")*/
    public static void clientSkipDialogues(ResourceLocation dialogues, String group, Integer index) {
        skipDialogues(dialogues, group, index);
    }

    //? forge
    /*@Info("客户端跳转对话，默认第一句话")*/
    public static void clientSkipDialogues(ResourceLocation dialogues, String group) {
        clientSkipDialogues(dialogues, group, 0);
    }

    //? forge
    /*@Info("获取当前对话的实体")*/
    public static List<Entity> serverGetChatTargets(ServerPlayer player) {
        return ChatBoxCommand.TARGETS_MAP.getOrDefault(player.getUUID(), List.of());
    }

    //? forge
    /*@Info("服务端打开最近打开的对话框，无视最大访问次数")*/
    public static void serverOpenChatBox(ServerPlayer player) {
        simplePayloadS2C(player, OPEN_DIALOG, "");
    }

    //? forge
    /*@Info("客户端打开最近打开的对话框，无视最大访问次数")*/
    public static void clientOpenChatBox() {
        if (dialoguesResourceLocation != null && group != null && index != null) {
            skipDialogues(dialoguesResourceLocation, group, index);
        }
    }

    //? forge
    /*@Info("服务端跳转下一条对话")*/
    public static void serverNextDialogue(ServerPlayer player) {
        simplePayloadS2C(player, NEXT_DIALOGUE, "");
    }

    //? forge
    /*@Info("客户端跳转下一条对话")*/
    public static void clientNextDialogue() {
        if (chatBoxScreen.shouldGotoNext()) skipDialogues(dialoguesResourceLocation, group, index + 1);
    }

    //? forge
    /*@Info("服务端开关自动对话")*/
    public static void serverAutoPlay(ServerPlayer player, boolean autoPlay) {
        simplePayloadS2C(player, AUTO_PLAY, String.valueOf(autoPlay));
    }

    //? forge
    /*@Info("客户端开关自动对话")*/
    public static void clientAutoPlay(boolean autoPlay) {
        chatBoxScreen.autoPlay = autoPlay;
    }

    //? forge
    /*@Info("服务端设置对话框是否为屏幕")*/
    public static void serverSetIsScreen(ServerPlayer player, boolean isScreen) {
        simplePayloadS2C(player, SET_IS_SCREEN, String.valueOf(isScreen));
    }

    //? forge
    /*@Info("客户端设置对话框是否为屏幕")*/
    public static void clientSetIsScreen(boolean isScreen) {
        ChatBoxUtil.isScreen = isScreen;
    }

    //? forge
    /*@Info("获取最大访问次数")*/
    public static int serverGetMaxTriggerCount(ServerPlayer player, ResourceLocation dialogResourceLocation) {
        return ChatBox.getTriggerCounts().getPlayerMaxTriggerCount(player, dialogResourceLocation);
    }

    //? forge
    /*@Info("设置最大访问次数")*/
    public static void serverSetMaxTriggerCount(ServerPlayer player, ResourceLocation dialogResourceLocation, int count) {
        ChatBox.getTriggerCounts().setPlayerMaxTriggerCount(player, dialogResourceLocation, count);
    }

    //? forge
    /*@Info("重置最大访问次数")*/
    public static void serverResetMaxTriggerCount(ServerPlayer player) {
        ChatBox.getTriggerCounts().resetPlayerMaxTriggerCount(player);
    }

    //? forge
    /*@Info("服务端设置对话框")*/
    public static void serverSetDialogBox(ServerPlayer player, String name, String text) {
        simplePayloadS2C(player, SET_DIALOG_BOX, StrUtil.merge(name, text));
    }

    //? forge
    /*@Info("客户端设置对话框")*/
    public static void clientSetDialogBox(String name, String text) {
        chatBoxScreen.dialogBox.setName(name).setText(text).setAllOver(false);
        //? >= 1.21 {
        var historicalInfo = historicalDialogue.historicalDialogue.historicalInfos.getLast();
        //?} else {
        /*var historicalInfos = historicalDialogue.historicalDialogue.historicalInfos;
        var historicalInfo = historicalInfos.get(historicalInfos.size() - 1);
        *///?}
        historicalInfo.name = name;
        historicalInfo.text = text;
    }

    //? forge
    /*@Info("服务端添加选项")*/
    public static void serverAddChatOption(ServerPlayer player, String text, String next, String tip, String clickType, String clickValue) {
        simplePayloadS2C(player, ADD_CHAT_OPTION, StrUtil.merge(text, next, tip, clickType, clickValue));
    }

    //? forge
    /*@Info("客户端添加选项")*/
    public static void clientAddChatOption(String text, String next, String tip, String clickType, String clickValue) {
        ChatOption option = chatBoxTheme.option.newOption()
                .setOptionChat(text).setNext(next).setOptionTooltip(tip).setClickEvent(clickType, clickValue);
        chatBoxScreen.addChatOptions(option);
    }

    //? forge
    /*@Info("服务端设置选项")*/
    public static void serverSetChatOption(ServerPlayer player, int index, String text, String tip, Boolean lock, Boolean hide) {
        simplePayloadS2C(player, SET_CHAT_OPTION, StrUtil.merge(index, text, tip, lock, hide));
    }

    //? forge
    /*@Info("客户端设置选项")*/
    public static void clientSetChatOption(int index, String text, String tip, Boolean lock, Boolean hide) {
        var options = chatBoxScreen.chatOptions;
        if (index < 0 || index >= options.size()) return;
        var option = options.get(index);
        option.setOptionChat(text).setOptionTooltip(tip).setIsLock(lock).hideOption(hide);
    }

    //? forge
    /*@Info("服务端清除选项")*/
    public static void serverClearChatOption(ServerPlayer player) {
        simplePayloadS2C(player, CLEAR_CHAT_OPTION, "");
    }

    //? forge
    /*@Info("客户端清除选项")*/
    public static void clientClearChatOption() {
        chatBoxScreen.chatOptions.clear();
    }

    //? forge
    /*@Info("注册一个组件事件，在服务端任意位置使用")*/
    public static void registerComponentEvent(String type, BiConsumer<AbstractComponent<?>, String> executeOnClient, boolean shouldExecuteOnServer, BiConsumer<ServerPlayer, String> executeOnServer) {
        EventExecutor.registerEvent(type, executeOnClient, () -> shouldExecuteOnServer, executeOnServer);
    }

    //? forge
    /*@Info("添加一个用于MVEL解析的动态方法，建议在启动脚本中调用，不过由于傻逼kjs的问题，这个方法无法正常工作")*/
    public static void addMvelMethod(String name, MVELUtil.DynamicMethod handler) {
        MVELUtil.registerMethod(name, handler);
    }

    //? forge
    /*@Info("添加一个用于MVEL解析的动态属性，建议在启动脚本中调用")*/
    public static void addMvelProperty(String name, MVELUtil.DynamicProperty handler) {
        MVELUtil.registerProperty(name, handler);
    }

    //? forge
    /*@Info("添加一个占位符解析器，建议在启动脚本中调用")*/
    public static void addPlaceholderResolver(String key, Function<Entity, Object> resolver) {
        MVELUtil.addPropertyResolver(key, resolver);
    }

    //? forge
    /*@Info("解析对话目标信息占位符")*/
    public static String parseTargetPlaceholders(ServerPlayer player, String input) {
        input = parsePlaceholders(player, input);
        return MVELUtil.parseTargetPlaceholders(player, input);
    }

    //? forge
    /*@HideFromJS*/
    public static String parsePlaceholders(ServerPlayer player, String input) {
        if (ChatBox.pluginHelper != null) input = ChatBox.pluginHelper.parsePapiPlaceholders(player.getUUID(), input);
        return input;
    }

    //? forge
    /*@HideFromJS*/
    public static void simplePayloadS2C(ServerPlayer player, String name, String value) {
        ChatBox.PLATFORM.sendToClient(player, new SimplePayload(name, value));
    }

    //? forge
    /*@HideFromJS*/
    public static void addSimpleHandlerS2C(String name, Consumer<String> handler) {
        SimplePayload.addHandlerS2C(name, handler);
    }

    //? forge
    /*@HideFromJS*/
    public static void simplePayloadC2S(String name, String value) {
        ChatBox.PLATFORM.sendToServer(new SimplePayload(name, value));
    }

    //? forge
    /*@HideFromJS*/
    public static void addSimpleHandlerC2S(String name, BiConsumer<ServerPlayer, String> handler) {
        SimplePayload.addHandlerC2S(name, handler);
    }
}
