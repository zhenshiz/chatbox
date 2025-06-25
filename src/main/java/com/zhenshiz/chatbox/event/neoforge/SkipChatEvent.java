package com.zhenshiz.chatbox.event.neoforge;

import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

@Getter
@AllArgsConstructor
public class SkipChatEvent extends Event {
    private final ChatBoxScreen chatBoxScreen;
    private final ResourceLocation resourceLocation;
    private final String group;
    private final Integer index;
}
