package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.api.ChatOptionClickEvent;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.*;

public class ChatBoxCommandUtil {
    public static final String SKIP_CHAT_C2S        = "skip_chat_c2s";

    public static final String OPEN_DIALOG          = "open_dialog";
    public static final String SET_THEME            = "set_theme";
    public static final String NEXT_DIALOGUE        = "next_dialogue";
    public static final String AUTO_PLAY            = "auto_play";
    public static final String SET_IS_SCREEN        = "set_is_screen";
    public static final String SET_DIALOG_BOX       = "set_dialog_box";
    public static final String ADD_CHAT_OPTION      = "add_chat_option";
    public static final String CLEAR_CHAT_OPTION    = "clear_chat_option";

    public static void serverToggleTheme(ServerPlayer player, ResourceLocation theme) {
        simplePayloadS2C(player, SET_THEME, theme.toString());
    }

    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Integer index, List<Entity> targets) {
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.OpenScreen(dialogues, group, index, entityListToString(targets)));
    }

    public static void serverSkipDialogues(ServerPlayer player, ResourceLocation dialogues, String group, Entity... targets) {
        serverSkipDialogues(player, dialogues, group, 0, List.of(targets));
    }

    public static List<Entity> serverGetChatTargets(ServerPlayer player) {
        return ChatBoxCommand.TARGETS_MAP.getOrDefault(player.getUUID(), List.of());
    }

    public static void serverOpenChatBox(ServerPlayer player) {
        simplePayloadS2C(player, OPEN_DIALOG, "");
    }

    public static void clientToggleTheme(String theme) {
        toggleTheme(new ResourceLocation(theme));
        themeResourceLocation = theme;
    }

    public static void clientSkipDialogues(ResourceLocation dialogues, String group, Integer index, String targets) {
        skipDialogues(dialogues, group, index, targets);
    }

    public static void clientSkipDialogues(ResourceLocation dialogues, String group, Integer index, List<Entity> targets) {
        skipDialogues(dialogues, group, index, targets);
    }

    public static void clientSkipDialogues(ResourceLocation dialogues, String group) {
        clientSkipDialogues(dialogues, group, 0, "");
    }

    public static void clientOpenChatBox() {
        if (dialoguesResourceLocation != null && group != null && index != null) {
            skipDialogues(dialoguesResourceLocation, group, index);
        }
    }

    public static void serverNextDialogue(ServerPlayer player) {
        simplePayloadS2C(player, NEXT_DIALOGUE, "");
    }

    public static void clientNextDialogue() {
        chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
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

    public static void serverSetDialogBox(ServerPlayer player, String name, String text) {
        simplePayloadS2C(player, SET_DIALOG_BOX, StrUtil.merge(name, text));
    }

    public static void clientSetDialogBox(String name, String text) {
        chatBoxScreen.dialogBox.setName(name, true).setText(text, true).resetTickCount().setAllOver(false);
    }

    public static void serverAddChatOption(ServerPlayer player, String text, String next, String tip, String clickType, String clickValue) {
        simplePayloadS2C(player, ADD_CHAT_OPTION, StrUtil.merge(text, next, tip, clickType, clickValue));
    }

    public static void clientAddChatOption(String text, String next, String tip, String clickType, String clickValue) {
        ChatOption option = new ChatOption().setOptionChat(text, true).setNext(next).setOptionTooltip(tip, true).setClickEvent(clickType, clickValue);
        chatBoxTheme.option.setChatOptionTheme(option, chatBoxScreen.chatOptions.size());
        chatBoxScreen.addChatOptions(option);
    }

    public static void serverClearChatOption(ServerPlayer player) {
        simplePayloadS2C(player, CLEAR_CHAT_OPTION, "");
    }

    public static void clientClearChatOption() {
        chatBoxScreen.chatOptions.clear();
    }

    public static void registerClickEvent(String type, Consumer<String> executeOnClient, boolean shouldExecuteOnServer, BiConsumer<ServerPlayer, String> executeOnServer) {
        ChatOptionClickEvent.registerClickEvent(type, executeOnClient, () -> shouldExecuteOnServer, executeOnServer);
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

    public static String entityListToString(List<Entity> entities) {
        if (entities.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (Entity entity : entities) {
            if (entity != null) builder.append(entity.getId()).append(", ");
        }
        return builder.substring(0, builder.length() - 2);
    }

    public static List<Entity> fromString(String entityList, Level level) {
        if (entityList.isEmpty()) return List.of();
        List<Entity> entities = new ArrayList<>();
        for (String id : entityList.split(", ")) {
            Entity entity = level.getEntity(Integer.parseInt(id));
            if (entity != null) entities.add(entity);
        }
        return entities;
    }

}
