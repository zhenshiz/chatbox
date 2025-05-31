package com.zhenshiz.chatbox.event;

import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;

public record SkipChatEvent(ChatBoxScreen chatBoxScreen, ResourceLocation resourceLocation, String group, Integer index) implements KubeEvent {
}
