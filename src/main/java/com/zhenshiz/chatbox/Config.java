package com.zhenshiz.chatbox;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@me.shedaniel.autoconfig.annotation.Config(name = "chatbox_config")
public class Config implements ConfigData {

    //历史记录界面鼠标滚动的速度
    @ConfigEntry.BoundedDiscrete(min = 1, max = 200)
    public int historicalScrollSpeed = 10;

}
