package com.zhenshiz.chatbox;

import com.zhenshiz.chatbox.event.KubeJSEvents;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;

public class ChatBoxKubePlugin implements KubeJSPlugin {
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(KubeJSEvents.GROUP);
    }
}
