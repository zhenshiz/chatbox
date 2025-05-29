package com.zhenshiz.chatbox;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec CONFIG_SPEC;

    //历史记录界面鼠标滚动的速度
    public static final ModConfigSpec.IntValue historicalScrollSpeed;

    static {
        ModConfigSpec.Builder CONFIG_BUILDER = new ModConfigSpec.Builder();
        CONFIG_BUILDER.push("config");
        historicalScrollSpeed = CONFIG_BUILDER.defineInRange("historical_scroll_speed", 10, 1, Integer.MAX_VALUE);
        CONFIG_BUILDER.pop();
        CONFIG_SPEC = CONFIG_BUILDER.build();
    }
}
