package com.zhenshiz.chatbox.compat.plugin;

import java.util.UUID;

public interface PluginHelper {

    default int executeCommand(UUID uuid, String command) {return 0;}

    default boolean isPapiLoaded() {return false;}

    default String parsePapiPlaceholders(UUID uuid, String text) {return text;}
}
