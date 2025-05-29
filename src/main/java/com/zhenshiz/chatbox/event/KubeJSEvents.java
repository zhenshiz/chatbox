package com.zhenshiz.chatbox.event;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface KubeJSEvents {
    EventGroup GROUP = EventGroup.of("ChatBoxEvents");

    EventHandler CHAT_BOX_RENDER_POST = GROUP.client("ChatBoxRenderPost", () -> ChatBoxRender.Post.class);
    EventHandler CHAT_BOX_RENDER_PRE = GROUP.client("ChatBoxRenderPre", () -> ChatBoxRender.Pre.class);
    EventHandler SKIP_CHAT = GROUP.client("SkipChat", () -> SkipChatEvent.class);
}
