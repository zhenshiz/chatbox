package com.zhenshiz.chatbox.event.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.GuiGraphics;

public class ChatBoxRenderEvent {
    public static Event<Pre> PRE = EventFactory.createArrayBacked(Pre.class,
            (listeners) -> (guiGraphics) -> {
                for (Pre event : listeners) {
                    if (event.pre(guiGraphics)) return true;
                }
                return false;
            });

    public static Event<Post> POST = EventFactory.createArrayBacked(Post.class,
            (listeners) -> (guiGraphics) -> {
                for (Post event : listeners) {
                    event.post(guiGraphics);
                }
            });

    public interface Pre {
        /**
         * @return true 取消对话框的渲染
         */
        boolean pre(GuiGraphics guiGraphics);
    }

    public interface Post {
        void post(GuiGraphics guiGraphics);
    }
}
