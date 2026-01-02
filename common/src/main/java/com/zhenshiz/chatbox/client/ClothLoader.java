package com.zhenshiz.chatbox.client;

import com.zhenshiz.chatbox.Config;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

@me.shedaniel.autoconfig.annotation.Config(name = "chatbox_config")
public class ClothLoader extends Config implements ConfigData {

    public static Config loadConfig() {
        AutoConfig.register(ClothLoader.class, Toml4jConfigSerializer::new);
        return AutoConfig.getConfigHolder(ClothLoader.class).getConfig();
    }
}
