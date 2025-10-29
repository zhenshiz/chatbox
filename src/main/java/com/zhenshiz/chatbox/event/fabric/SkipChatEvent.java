package com.zhenshiz.chatbox.event.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public interface SkipChatEvent {
    Event<SkipChatEvent> EVENT = EventFactory.createArrayBacked(SkipChatEvent.class,
            (listeners) -> (player, res, group, index, targets) -> {
                for (SkipChatEvent event : listeners) {
                    event.skipChat(player, res, group, index, targets);
                }
            });

    void skipChat(Player player, ResourceLocation resourceLocation, String group, int index, List<Entity> targets);
}
