package com.zhenshiz.chatbox.neoforge.nekojs;

import com.tkisor.nekojs.api.event.EventBusForgeBridge;
import com.tkisor.nekojs.api.event.EventBusJS;
import com.tkisor.nekojs.api.event.EventGroup;
import com.zhenshiz.chatbox.neoforge.event.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.neoforge.event.SkipChatEvent;
import net.neoforged.neoforge.common.NeoForge;

public interface ChatBoxEventsJS {
    EventGroup GROUP = EventGroup.of("ChatBoxEvents");

    EventBusJS<ChatBoxRenderEvent.Pre, Void> RENDER_PRE = GROUP.client("renderPre", ChatBoxRenderEvent.Pre.class);
    EventBusJS<ChatBoxRenderEvent.Post, Void> RENDER_POST = GROUP.client("renderPost", ChatBoxRenderEvent.Post.class);
    EventBusJS<SkipChatEvent, Void> SKIP_CHAT = GROUP.common("skipChat", SkipChatEvent.class);
    EventBusForgeBridge FORGE_BRIDGE = EventBusForgeBridge.create(NeoForge.EVENT_BUS).bind(RENDER_PRE).bind(RENDER_POST).bind(SKIP_CHAT);
}
