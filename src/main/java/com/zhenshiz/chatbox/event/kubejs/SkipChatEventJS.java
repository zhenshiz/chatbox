package com.zhenshiz.chatbox.event.kubejs;

import com.zhenshiz.chatbox.event.neoforge.SkipChatEvent;
import dev.latvian.mods.kubejs.event.KubeEvent;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

@Getter
public class SkipChatEventJS implements KubeEvent {
    private final Player player;
    private final ResourceLocation resourceLocation;
    private final String group;
    private final int index;
    private final List<Entity> targets;

    public SkipChatEventJS(SkipChatEvent event) {
        player = event.getPlayer();
        resourceLocation = event.getResourceLocation();
        group = event.getGroup();
        index = event.getIndex();
        targets = event.getTargets();
    }
}
