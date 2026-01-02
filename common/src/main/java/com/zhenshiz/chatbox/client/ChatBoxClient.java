package com.zhenshiz.chatbox.client;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.Config;

public class ChatBoxClient {
    public static Config conf;

    public static void init() {
        if (ChatBox.isClothConfigLoaded()) conf = ClothLoader.loadConfig();
        else conf = new Config();
    }
}
