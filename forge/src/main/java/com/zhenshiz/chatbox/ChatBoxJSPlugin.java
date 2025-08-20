package com.zhenshiz.chatbox;

import com.zhenshiz.chatbox.event.ChatBoxEventsJS;
import com.zhenshiz.chatbox.event.CommonEventsPostJS;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class ChatBoxJSPlugin extends KubeJSPlugin {
    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(CommonEventsPostJS.class);
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        if (FMLEnvironment.dist.isClient()) {
            event.add("ChatBoxUtil", ChatBoxCommandUtil.class);
        }
    }

    @Override
    public void registerEvents() {
        ChatBoxEventsJS.GROUP.register();
    }
}
