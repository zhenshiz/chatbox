package com.zhenshiz.chatbox.event.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class InputEvent {

    public static class MouseButton {
        public static Event<Pre> PRE = EventFactory.createArrayBacked(Pre.class,
                (listeners) -> (button, action, modifiers) -> {
                    for (Pre event : listeners) {
                        if (event.mousePre(button, action, modifiers)) return true;
                    }
                    return false;
                });

        public static Event<Post> POST = EventFactory.createArrayBacked(Post.class,
                (listeners) -> (button, action, modifiers) -> {
                    for (Post event : listeners) {
                        event.mousePost(button, action, modifiers);
                    }
                });

        public interface Pre {
            boolean mousePre(int button, int action, int modifiers);
        }

        public interface Post {
            void mousePost(int button, int action, int modifiers);
        }
    }

    public static Event<MouseScrollingEvent> MOUSE_SCROLLING = EventFactory.createArrayBacked(MouseScrollingEvent.class,
            (listeners) -> (scrollDeltaX, scrollDeltaY, leftDown, middleDown, rightDown, mouseX, mouseY) -> {
                for (MouseScrollingEvent event : listeners) {
                    if (event.onMouseScroll(scrollDeltaX, scrollDeltaY, leftDown, middleDown, rightDown, mouseX, mouseY)) return true;
                }
                return false;
            });

    public interface MouseScrollingEvent {
        boolean onMouseScroll(double scrollDeltaX, double scrollDeltaY, boolean leftDown, boolean middleDown, boolean rightDown, double mouseX, double mouseY);
    }

    public static Event<Key> KEY = EventFactory.createArrayBacked(Key.class,
            (listeners) -> (key, scancode, action, modifiers) -> {
                for (Key event : listeners) {
                    event.onKey(key, scancode, action, modifiers);
                }
            });

    public interface Key {
        void onKey(int key, int scancode, int action, int modifiers);
    }
}
