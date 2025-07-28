package com.zhenshiz.chatbox.event.kubejs;

import com.zhenshiz.chatbox.event.neoforge.ChatBoxRenderEvent;
import dev.latvian.mods.kubejs.event.KubeEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

@Getter
@AllArgsConstructor
public abstract class ChatBoxRenderEventJS implements KubeEvent {
    private final GuiGraphics guiGraphics;

    public static class Pre extends ChatBoxRenderEventJS {
        public Pre(ChatBoxRenderEvent.Pre event) {
            super(event.getGuiGraphics());
        }
    }

    public static class Post extends ChatBoxRenderEventJS {
        public Post(ChatBoxRenderEvent.Post event) {
            super(event.getGuiGraphics());
        }
    }
}
