package com.zhenshiz.chatbox.network;

import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class NetworkFabric {

    public static void registerServerHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(SendClickEvent.ID, (server, player, h, buf, r) -> SendClickEvent.handleOnServer(player, SendClickEvent.decode(buf)));
        ServerPlayNetworking.registerGlobalReceiver(SimplePayload.ID, (server, player, h, buf, r) -> SimplePayload.handleOnServer(player, SimplePayload.decode(buf)));
    }
}
