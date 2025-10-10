package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.api.ChatOptionClickEvent;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.network.c2s.ServerChatBoxPayload;
import com.zhenshiz.chatbox.network.s2c.ClientChatBoxPayload;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.*;

public class ChatBoxCommandUtil {
    @Info("服务端切换对话框主题样式")
    public static void serverToggleTheme(ServerPlayer player, ResourceLocation theme) {
        if (player != null) player.connection.send(new ClientChatBoxPayload.ToggleTheme(theme));
    }

    @Info("服务端跳转对话，自带同步数据附件")
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index) {
        if (player != null) {
            player.connection.send(new ClientChatBoxPayload.OpenScreenPayload(dialogues, group, index));
        }
    }

    @Info("服务端跳转对话，默认第一句话")
    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group) {
        serverSkipDialogues(player, dialogues, group, 0);
    }

    @Info("服务端打开对话框，无视最大访问次数")
    public static void serverOpenChatBox(ServerPlayer player) {
        if (player != null) player.connection.send(new ClientChatBoxPayload.OpenChatBox());
    }

    @Info("""
            服务端设置最大访问次数，不包含同步
            
            需要自行发包保证双端同步
            
            player.connection.send(new ClientChatBoxPayload.SetMaxTriggerCount(ResourceLocation,int));
            """)
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

    @Info("""
            服务端重置访问次数，不包含同步
            
            需要自行发包保证双端同步
            
            player.connection.send(new ClientChatBoxPayload.ResetMaxTriggerCount());
            """)
    public static void serverResetMaxTriggerCount(ServerPlayer player) {
        if (player != null) {
            player.setData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT, new ChatBoxTriggerCount.MaxTriggerCount());
        }
    }

    @Info("客户端切换对话框主题样式")
    public static void clientToggleTheme(ResourceLocation theme) {
        toggleTheme(theme);
        themeResourceLocation = theme.toString();
    }

    @Info("客户端跳转对话，自带同步数据附件")
    public static void clientSkipDialogues(ResourceLocation dialoguesResourceLocation, String group, Integer index) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        //判断该对话是否有触发的次数限制
        ChatBoxDialogues chatBoxDialogues = dialoguesMap.get(dialoguesResourceLocation);
        ChatBoxTriggerCount.MaxTriggerCount maxTriggerCount = minecraft.player.getData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT);
        String resourceLocation = dialoguesResourceLocation.toString();
        Map<String, Integer> triggerCounts = maxTriggerCount.getTriggerCounts();
        Integer count = triggerCounts.get(resourceLocation);
        if (count == null) {
            clientSetMaxTriggerCount(dialoguesResourceLocation, chatBoxDialogues.maxTriggerCount - 1);
            minecraft.player.connection.send(new ServerChatBoxPayload.SetMaxTriggerCountPayload(dialoguesResourceLocation, chatBoxDialogues.maxTriggerCount - 1));
        } else {
            if (count == 0) return;
            clientSetMaxTriggerCount(dialoguesResourceLocation, count - 1);
            minecraft.player.connection.send(new ServerChatBoxPayload.SetMaxTriggerCountPayload(dialoguesResourceLocation, count - 1));
        }

        String theme = chatBoxDialogues.theme;
        if (theme != null && !theme.equals(themeResourceLocation)) clientToggleTheme(ResourceLocation.parse(theme));
        skipDialogues(dialoguesResourceLocation, group, index);
    }

    @Info("客户端跳转对话，默认第一句话")
    public static void clientSkipDialogues(ResourceLocation dialogues, String group) {
        clientSkipDialogues(dialogues, group, 0);
    }

    @Info("客户端打开对话框，无视最大访问次数")
    public static void clientOpenChatBox() {
        if (Minecraft.getInstance().player != null && dialoguesResourceLocation != null && group != null && index != null) {
            skipDialogues(dialoguesResourceLocation, group, index);
        }
    }

    @Info("""
            客户端设置最大访问次数，不包含同步
            
            需要自行发包保证双端同步
            
            player.connection.send(new ServerChatBoxPayload.SetMaxTriggerCountPayload(ResourceLocation,int));
            """)
    public static void clientSetMaxTriggerCount(ResourceLocation dialogResourceLocation, int count) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ChatBoxTriggerCount.MaxTriggerCount maxTriggerCount = minecraft.player.getData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT);
        String resourceLocation = dialogResourceLocation.toString();
        Map<String, Integer> triggerCounts = maxTriggerCount.getTriggerCounts();
        triggerCounts.put(resourceLocation, count);
        minecraft.player.setData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT, maxTriggerCount);
    }

    @Info("""
            客户端重置访问次数，不包含同步
            
            需要自行发包保证双端同步
            
            player.connection.send(new ServerChatBoxPayload.ResetMaxTriggerCount());
            """)
    public static void clientResetMaxTriggerCount() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.player.setData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT, new ChatBoxTriggerCount.MaxTriggerCount());
    }

    @Info("跳转下一条对话")
    public static void clientNextDialogue() {
        chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
    }

    @Info("开关自动对话")
    public static void clientAutoPlay(boolean autoPlay) {
        chatBoxScreen.autoPlay = autoPlay;
    }

    @Info("注册一个选项点击事件")
    public static void registerClickEvent(String type, Consumer<String> executeOnClient, Boolean shouldExecuteOnServer, BiConsumer<ServerPlayer, String> executeOnServer) {
        ChatOptionClickEvent.registerClickEvent(type, executeOnClient, shouldExecuteOnServer, executeOnServer);
    }

    @Info("切换对话框是否为屏幕")
    public static void clientToggleIsScreen(boolean isScreen) {
        ChatBoxUtil.isScreen = isScreen;
    }
}
