package com.zhenshiz.chatbox.compat.plugin;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.DialogBox;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static com.zhenshiz.chatbox.ChatBox.pluginHelper;
import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil.*;

public interface PluginHelper {

    default int executeCommand(UUID uuid, String command) {return 0;}

    default boolean isPapiLoaded() {return false;}

    default String parsePapiPlaceholders(UUID uuid, String text) {return text;}

    String RESOLVE_PAPI         = "resolve_papi";
    String SET_PAPI_LOADED      = "set_papi_loaded";

    static void init() {
        addSimpleHandlerC2S(RESOLVE_PAPI, (player, s) -> {
            String[] parts = StrUtil.parse(s);
            if (parts.length != 2) return;

            ChatBox.LOGGER.info("Resolving Placeholder for {}: {} -> {}", player, parts[0], parts[1]);
            String name = parts[0]; String text = parts[1];
            if (pluginHelper != null) {
                name = pluginHelper.parsePapiPlaceholders(player.getUUID(), name);
                text = pluginHelper.parsePapiPlaceholders(player.getUUID(), text);
            }
            serverSetDialogBox(player, name, text);
        });
        addSimpleHandlerS2C(SET_PAPI_LOADED, s -> DialogBox.papiLoaded = Boolean.parseBoolean(s));
    }

    static void setPapiLoaded(ServerPlayer player) {
        if (pluginHelper == null) return;
        simplePayloadS2C(player, SET_PAPI_LOADED, Boolean.toString(pluginHelper.isPapiLoaded()));
    }
}
