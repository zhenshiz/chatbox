package com.zhenshiz.chatbox;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class Config {

    //历史记录界面鼠标滚动的速度
    @ConfigEntry.BoundedDiscrete(min = 1, max = 200)
    public int historicalScrollSpeed = 10;

    @ConfigEntry.Gui.Tooltip()
    public boolean soundInterruptionEnabled = true;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 721)
    public int charPerSecond = 20;

}
