package com.zhenshiz.chatbox.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ChatBoxRender implements KubeEvent {
    private final GuiGraphics guiGraphics;

    public ChatBoxRender(GuiGraphics guiGraphics) {
        this.guiGraphics = guiGraphics;
    }

    public GuiGraphics getGuiGraphics() {
        return this.guiGraphics;
    }

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
