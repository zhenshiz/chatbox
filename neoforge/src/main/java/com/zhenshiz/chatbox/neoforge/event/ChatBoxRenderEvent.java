package com.zhenshiz.chatbox.neoforge.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
@AllArgsConstructor
public abstract class ChatBoxRenderEvent extends Event {
    private final GuiGraphicsExtractor guiGraphics;

    public static class Pre extends ChatBoxRenderEvent implements ICancellableEvent {
        public Pre(GuiGraphicsExtractor guiGraphics) {
            super(guiGraphics);
        }
    }

    public static class Post extends ChatBoxRenderEvent {
        public Post(GuiGraphicsExtractor guiGraphics) {
            super(guiGraphics);
        }
    }
}
