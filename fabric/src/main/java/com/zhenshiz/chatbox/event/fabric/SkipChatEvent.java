package com.zhenshiz.chatbox.event.fabric;

import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;

public interface SkipChatEvent {
    Event<SkipChatEvent> EVENT = EventFactory.createArrayBacked(SkipChatEvent.class,
            (listeners) -> (screen, res, group, index) -> {
                for (SkipChatEvent event : listeners) {
                    event.skipChat(screen, res, group, index);
                }
            });

    void skipChat(ChatBoxScreen chatBoxScreen, ResourceLocation resourceLocation, String group, Integer index);
}
