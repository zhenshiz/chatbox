package com.zhenshiz.chatbox;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec CONFIG_SPEC;

    //历史记录界面鼠标滚动的速度
    public static final ModConfigSpec.IntValue historicalScrollSpeed;

    //下句话如果没有新音效，是否中断前面正在播放的音效
    public static final ModConfigSpec.BooleanValue soundInterruptionEnabled;

    //每秒显示的字符数
    public static final ModConfigSpec.IntValue charPerSecond;

    //是否阻拦TerraNpc的对话系统
    public static ModConfigSpec.BooleanValue isStopTerraDialog = null;

    static {
        ModConfigSpec.Builder CONFIG_BUILDER = new ModConfigSpec.Builder();
        CONFIG_BUILDER.push("config");
        historicalScrollSpeed = CONFIG_BUILDER.defineInRange("historical_scroll_speed", 10, 1, Integer.MAX_VALUE);
        soundInterruptionEnabled = CONFIG_BUILDER.define("sound_interruption_enabled", true);
        charPerSecond = CONFIG_BUILDER.defineInRange("char_per_second", 20, 1, Integer.MAX_VALUE);
        if (ChatBox.isTerraEntityLoaded()) {
            isStopTerraDialog = CONFIG_BUILDER.define("is_stop_terra_dialog", false);
        }
        CONFIG_BUILDER.pop();
        CONFIG_SPEC = CONFIG_BUILDER.build();
    }
}
