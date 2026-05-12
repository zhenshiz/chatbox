package com.zhenshiz.chatbox.client;

import com.zhenshiz.chatbox.Config;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

public class ChatBoxClient {
    public static Config conf;

    public static void init() {
        AutoConfig.register(Config.class, Toml4jConfigSerializer::new);
        conf = AutoConfig.getConfigHolder(Config.class).getConfig();
    }
}
