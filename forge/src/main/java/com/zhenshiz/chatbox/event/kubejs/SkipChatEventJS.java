package com.zhenshiz.chatbox.event.kubejs;

import com.zhenshiz.chatbox.event.forge.SkipChatEvent;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import dev.latvian.mods.kubejs.event.EventJS;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

@Getter
public class SkipChatEventJS extends EventJS {
    private final ChatBoxScreen chatBoxScreen;
    private final ResourceLocation resourceLocation;
    private final String group;
    private final Integer index;

    public SkipChatEventJS(SkipChatEvent event) {
        chatBoxScreen = event.getChatBoxScreen();
        resourceLocation = event.getResourceLocation();
        group = event.getGroup();
        index = event.getIndex();
    }
}
