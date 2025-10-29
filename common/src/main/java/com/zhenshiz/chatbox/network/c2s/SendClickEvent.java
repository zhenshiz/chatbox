package com.zhenshiz.chatbox.network.c2s;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.api.ChatOptionClickEvent;
import com.zhenshiz.chatbox.network.CustomPacket;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record SendClickEvent(String type, String value) implements CustomPacket {
    public ResourceLocation id() {return ID;}
    public static final ResourceLocation ID = ChatBox.ResourceLocationMod("execute_click_event");

    public void write(FriendlyByteBuf buf) {encode(this, buf);}

    public static void encode(SendClickEvent packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.type);
        // 解析目标实体占位符
        buf.writeUtf(ChatBoxUtil.parseTargetPlaceholders(packet.value));
    }

    public static SendClickEvent decode(FriendlyByteBuf buf) {
        return new SendClickEvent(buf.readUtf(), buf.readUtf());
    }

    public static void handleOnServer(ServerPlayer player, SendClickEvent packet) {
        ChatOptionClickEvent.CLICK_EVENTS.get(packet.type()).executeOnServer(player, packet.value());
    }
}
