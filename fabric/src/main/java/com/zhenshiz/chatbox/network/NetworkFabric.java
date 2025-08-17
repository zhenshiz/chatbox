package com.zhenshiz.chatbox.network;

import com.zhenshiz.chatbox.network.c2s.SendCommandPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class NetworkFabric {

    public static void registerServerHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(SendCommandPayload.ID, (server, player, h, buf, r) -> SendCommandPayload.handleOnServer(server, player, SendCommandPayload.decode(buf)));
    }
}
