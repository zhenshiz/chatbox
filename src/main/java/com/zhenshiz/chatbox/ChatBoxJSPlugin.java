package com.zhenshiz.chatbox;

import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import net.neoforged.fml.loading.FMLEnvironment;

public class ChatBoxJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (FMLEnvironment.dist.isClient()) {
            bindings.add("ChatBoxUtil", ChatBoxCommandUtil.class);
        }
    }
}
