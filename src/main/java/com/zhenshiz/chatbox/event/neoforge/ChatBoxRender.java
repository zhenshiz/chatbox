package com.zhenshiz.chatbox.event.neoforge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
@AllArgsConstructor
public abstract class ChatBoxRender extends Event {
    private final GuiGraphics guiGraphics;

    public static class Pre extends ChatBoxRender implements ICancellableEvent {
        public Pre(GuiGraphics guiGraphics) {
            super(guiGraphics);
        }
    }

    public static class Post extends ChatBoxRender {
        public Post(GuiGraphics guiGraphics) {
            super(guiGraphics);
        }
    }
}
