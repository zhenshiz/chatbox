package com.zhenshiz.chatbox.neoforge.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

import java.util.List;

@Getter
@AllArgsConstructor
public class SkipChatEvent extends Event {
    private final Player player;
    private final Identifier identifier;
    private final String group;
    private final int index;
    private final List<Entity> targets;
}
