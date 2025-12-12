package com.zhenshiz.chatbox.event.neoforge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
@AllArgsConstructor
public abstract class ChatBoxRenderEvent extends Event {
    private final GuiGraphics guiGraphics;

    public static class Pre extends ChatBoxRenderEvent implements ICancellableEvent {
        public Pre(GuiGraphics guiGraphics) {
            super(guiGraphics);
        }
    }

    public static class Post extends ChatBoxRenderEvent {
        public Post(GuiGraphics guiGraphics) {
            super(guiGraphics);
        }
    }
}
