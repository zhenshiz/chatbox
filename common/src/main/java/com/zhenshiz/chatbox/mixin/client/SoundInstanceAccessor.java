package com.zhenshiz.chatbox.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(SoundEngine.class)
public interface SoundInstanceAccessor {

    @Accessor("instanceToChannel")
    Map<SoundInstance, ChannelAccess.ChannelHandle> getInstanceToChannel();

    @Invoker("tickInGameSound")
    void invokeTickNonPaused();
}
