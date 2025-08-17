package com.zhenshiz.chatbox.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface CustomPacket {

    ResourceLocation id();

    void write(FriendlyByteBuf buf);
}
