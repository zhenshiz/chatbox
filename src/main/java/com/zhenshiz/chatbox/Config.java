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

    //图片类型立绘的宽度缩放比例，200表示是原本的两倍宽，50表示原本的一半宽
    public static final ModConfigSpec.IntValue portraitWidthPercent;

    //是否禁用鼠标滚动动作
    public static final ModConfigSpec.BooleanValue disableMouseScroll;

    //是否阻拦TerraNpc的对话系统
    public static ModConfigSpec.BooleanValue isStopTerraDialog = null;

    static {
        ModConfigSpec.Builder CONFIG_BUILDER = new ModConfigSpec.Builder();
        CONFIG_BUILDER.push("config");
        historicalScrollSpeed = CONFIG_BUILDER.defineInRange("historical_scroll_speed", 10, 1, Integer.MAX_VALUE);
        soundInterruptionEnabled = CONFIG_BUILDER.define("sound_interruption_enabled", true);
        charPerSecond = CONFIG_BUILDER.defineInRange("char_per_second", 20, 1, Integer.MAX_VALUE);
        portraitWidthPercent = CONFIG_BUILDER.defineInRange("portrait_width_percent", 100, 1, 200);
        disableMouseScroll = CONFIG_BUILDER.comment("Disable mouse scroll action in the chatbox screen (scroll up for log, down as mouse click)", "禁用对话框屏幕的：向上滚动到历史记录，向下滚动同鼠标点击").define("disable_mouse_scroll", false);
        if (ChatBox.isTerraEntityLoaded()) {
            isStopTerraDialog = CONFIG_BUILDER.define("is_stop_terra_dialog", false);
        }
        CONFIG_BUILDER.pop();
        CONFIG_SPEC = CONFIG_BUILDER.build();
    }
}
