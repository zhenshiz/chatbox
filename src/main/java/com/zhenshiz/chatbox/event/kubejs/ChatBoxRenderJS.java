package com.zhenshiz.chatbox.event.kubejs;

import com.zhenshiz.chatbox.event.neoforge.ChatBoxRender;
import dev.latvian.mods.kubejs.event.KubeEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

@Getter
@AllArgsConstructor
public abstract class ChatBoxRenderJS implements KubeEvent {
    private final GuiGraphics guiGraphics;

    public static class Pre extends ChatBoxRenderJS {
        public Pre(ChatBoxRender.Pre event) {
            super(event.getGuiGraphics());
        }
    }

    public static class Post extends ChatBoxRenderJS {
        public Post(ChatBoxRender.Post event) {
            super(event.getGuiGraphics());
        }
    }
}
