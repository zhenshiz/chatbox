/*
package com.zhenshiz.chatbox.neoforge.event.nekojs;

import com.zhenshiz.chatbox.neoforge.event.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.neoforge.event.SkipChatEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class CommonEventsPostJS {

    public static class Common {
        @SubscribeEvent(priority = EventPriority.LOW)
        public static void chatBoxSkipChat(SkipChatEvent event) {
            ChatBoxEventsJS.CHAT_BOX_SKIP_CHAT.post(new SkipChatEventJS(event));
        }
    }

    public static class Client {
        @SubscribeEvent(priority = EventPriority.LOW)
        public static void chatBoxRenderPre(ChatBoxRenderEvent.Pre event) {
            var pre = new ChatBoxRenderEventJS.Pre(event);
            ChatBoxEventsJS.CHAT_BOX_RENDER_PRE.post(pre, event.isCanceled());
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void chatBoxRenderPost(ChatBoxRenderEvent.Post event) {
            ChatBoxEventsJS.CHAT_BOX_RENDER_POST.post(new ChatBoxRenderEventJS.Post(event));
        }
    }
}
*/
