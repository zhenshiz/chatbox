package com.zhenshiz.chatbox.event.neoforge;

import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

@Getter
@AllArgsConstructor
public class SkipChatEvent extends Event {
    public final ChatBoxScreen chatBoxScreen;
    public final ResourceLocation resourceLocation;
    public final String group;
    public final Integer index;
}
