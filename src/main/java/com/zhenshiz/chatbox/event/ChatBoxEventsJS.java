package com.zhenshiz.chatbox.event;

import com.zhenshiz.chatbox.event.kubejs.ChatBoxRenderEventJS;
import com.zhenshiz.chatbox.event.kubejs.SkipChatEventJS;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface ChatBoxEventsJS {
    EventGroup GROUP = EventGroup.of("ChatBoxEvents");

    EventHandler CHAT_BOX_RENDER_PRE = GROUP.client("renderPre", () -> ChatBoxRenderEventJS.Pre.class).hasResult();
    EventHandler CHAT_BOX_RENDER_POST = GROUP.client("renderPost", () -> ChatBoxRenderEventJS.Post.class);
    EventHandler CHAT_BOX_SKIP_CHAT = GROUP.client("skipChat", () -> SkipChatEventJS.class);
}
