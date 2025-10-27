package com.zhenshiz.chatbox.event.neoforge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

@Getter
@AllArgsConstructor
public class SkipChatEvent extends Event {
    private final Player player;
    private final ResourceLocation resourceLocation;
    private final String group;
    private final Integer index;
}
