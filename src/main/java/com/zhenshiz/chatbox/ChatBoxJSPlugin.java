package com.zhenshiz.chatbox;

import com.zhenshiz.chatbox.event.ChatBoxEventsJS;
import com.zhenshiz.chatbox.event.CommonEventsPostJS;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

public class ChatBoxJSPlugin implements KubeJSPlugin {
    @Override
    public void init() {
        NeoForge.EVENT_BUS.register(CommonEventsPostJS.class);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (FMLEnvironment.dist.isClient()) {
            bindings.add("ChatBoxUtil", ChatBoxCommandUtil.class);
        }
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(ChatBoxEventsJS.GROUP);
    }
}
