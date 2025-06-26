package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.component.DialogBox;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.payload.c2s.SetMaxTriggerCountPayload;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class ChatBoxCommandUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();
    public static String themeResourceLocation = null;

    public static void serverToggleTheme(ServerPlayer player, ResourceLocation theme) {
        if (player != null) player.connection.send(new ChatBoxPayload.ToggleTheme(theme));
    }

    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index) {
        if (player != null) player.connection.send(new ChatBoxPayload.OpenScreenPayload(dialogues, group, index));
    }

    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group) {
        serverSkipDialogues(player, dialogues, group, 0);
    }

    public static void serverOpenChatBox(ServerPlayer player) {
        if (player != null) player.connection.send(new ChatBoxPayload.OpenChatBox());
    }

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

    public static void serverResetMaxTriggerCount(ServerPlayer player) {
        if (player != null) {
            player.setData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT, new ChatBoxTriggerCount.MaxTriggerCount());
        }
    }

    public static void clientToggleTheme(ResourceLocation theme) {
        ChatBoxUtil.toggleTheme(theme);
        themeResourceLocation = theme.toString();
    }

    public static void clientSkipDialogues(ResourceLocation dialoguesResourceLocation, String group, Integer index) {
        if (minecraft.player == null) return;

        ChatBoxDialogues chatBoxDialogues = ChatBoxUtil.dialoguesMap.get(dialoguesResourceLocation);
        //判断该对话是否有触发的次数限制
        if (chatBoxDialogues.maxTriggerCount == 0) {
            return;
        } else if (chatBoxDialogues.maxTriggerCount > 0) {
            ChatBoxTriggerCount.MaxTriggerCount maxTriggerCount = minecraft.player.getData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT);
            String resourceLocation = dialoguesResourceLocation.toString();
            Map<String, Integer> triggerCounts = maxTriggerCount.getTriggerCounts();
            Integer count = triggerCounts.get(resourceLocation);
            if (count == null) {
                triggerCounts.put(resourceLocation, chatBoxDialogues.maxTriggerCount - 1);
                minecraft.player.connection.send(new SetMaxTriggerCountPayload(dialoguesResourceLocation, chatBoxDialogues.maxTriggerCount - 1));
            } else {
                if (count == 0) return;
                triggerCounts.put(resourceLocation, count - 1);
                minecraft.player.connection.send(new SetMaxTriggerCountPayload(dialoguesResourceLocation, count - 1));
            }
            minecraft.player.setData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT, maxTriggerCount);
        }
        ChatBoxUtil.skipDialogues(dialoguesResourceLocation, group, index);
    }

    public static void clientSkipDialogues(ResourceLocation dialogues, String group) {
        clientSkipDialogues(dialogues, group, 0);
    }

    public static void clientOpenChatBox() {
        if (minecraft.player != null) {
            DialogBox dialogBox = ChatBoxUtil.chatBoxScreen.dialogBox;
            ResourceLocation dialoguesResourceLocation = dialogBox.dialoguesResourceLocation;
            String group = dialogBox.group;
            Integer index = dialogBox.index;
            if (dialoguesResourceLocation != null && group != null && index != null) {
                ChatBoxUtil.skipDialogues(dialoguesResourceLocation, group, index);
            }
        }
    }

    public static void clientSetMaxTriggerCount(ResourceLocation dialogResourceLocation, int count) {
        if (minecraft.player == null) return;
        ChatBoxTriggerCount.MaxTriggerCount maxTriggerCount = minecraft.player.getData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT);
        String resourceLocation = dialogResourceLocation.toString();
        Map<String, Integer> triggerCounts = maxTriggerCount.getTriggerCounts();
        triggerCounts.put(resourceLocation, count);
        minecraft.player.setData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT, maxTriggerCount);
    }

    public static void clientResetMaxTriggerCount() {
        if (minecraft.player == null) return;
        minecraft.player.setData(ChatBoxTriggerCount.MAX_TRIGGER_COUNT, new ChatBoxTriggerCount.MaxTriggerCount());
    }
}
