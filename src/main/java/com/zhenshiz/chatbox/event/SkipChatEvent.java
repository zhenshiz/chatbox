package com.zhenshiz.chatbox.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;

public record SkipChatEvent(ResourceLocation resourceLocation, String group, Integer index) implements KubeEvent {
}
