package com.zhenshiz.chatbox.event.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public interface SkipChatEvent {
    Event<SkipChatEvent> EVENT = EventFactory.createArrayBacked(SkipChatEvent.class,
            (listeners) -> (player, resourceLocation, group, index, targets) -> {
                for (SkipChatEvent event : listeners) {
                    event.skipChat(player, resourceLocation, group, index, targets);
                }
            });

    void skipChat(Player player, ResourceLocation resourceLocation, String group, Integer index, List<Entity> targets);
}
