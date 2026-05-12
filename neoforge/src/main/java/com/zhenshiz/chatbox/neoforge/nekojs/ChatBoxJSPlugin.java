package com.zhenshiz.chatbox.neoforge.nekojs;

import com.tkisor.nekojs.api.NekoJSPlugin;
import com.tkisor.nekojs.api.annotation.RegisterNekoJSPlugin;
import com.tkisor.nekojs.api.data.Binding;
import com.tkisor.nekojs.api.data.BindingsRegister;
import com.tkisor.nekojs.api.event.EventGroupRegistry;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;

@RegisterNekoJSPlugin
public class ChatBoxJSPlugin implements NekoJSPlugin {

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(ChatBoxEventsJS.GROUP);
    }

    @Override
    public void registerBindings(BindingsRegister registry) {
        registry.register(Binding.of("ChatBoxUtil", ChatBoxCommandUtil.class));
    }
}
