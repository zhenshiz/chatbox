package com.zhenshiz.chatbox.event.forge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Getter
@AllArgsConstructor
public abstract class ChatBoxRenderEvent extends Event {
    private final GuiGraphics guiGraphics;

    @Cancelable
    public static class Pre extends ChatBoxRenderEvent {
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
