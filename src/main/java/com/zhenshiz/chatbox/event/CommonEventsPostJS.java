package com.zhenshiz.chatbox.event;

import com.zhenshiz.chatbox.event.kubejs.ChatBoxRenderJS;
import com.zhenshiz.chatbox.event.kubejs.SkipChatEventJS;
import com.zhenshiz.chatbox.event.neoforge.ChatBoxRender;
import com.zhenshiz.chatbox.event.neoforge.SkipChatEvent;
import dev.latvian.mods.kubejs.event.EventResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class CommonEventsPostJS {
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void chatBoxRenderPre(ChatBoxRender.Pre event) {
        if (ChatBoxEventsJS.CHAT_BOX_RENDER_PRE.hasListeners()) {
            EventResult result = ChatBoxEventsJS.CHAT_BOX_RENDER_PRE.post(new ChatBoxRenderJS.Pre(event));
            if (result.interruptFalse()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void chatBoxRenderPost(ChatBoxRender.Post event) {
        if (ChatBoxEventsJS.CHAT_BOX_RENDER_POST.hasListeners()) {
            ChatBoxEventsJS.CHAT_BOX_RENDER_POST.post(new ChatBoxRenderJS.Post(event));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void chatBoxSkipChat(SkipChatEvent event) {
        if (ChatBoxEventsJS.CHAT_BOX_SKIP_CHAT.hasListeners()) {
            ChatBoxEventsJS.CHAT_BOX_SKIP_CHAT.post(new SkipChatEventJS(event));
        }
    }
}
