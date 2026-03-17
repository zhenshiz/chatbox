package com.zhenshiz.chatbox.fabric.client;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.fabric.event.InputEvent;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.render.ChatBoxRender;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class ClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ChatBoxClient.init();
        registerClientHandlers();
        registerRenderEvents();
    }

    public static void registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.ChatBoxDataToClient.TYPE, ((packet, context) -> ChatBoxPayload.ChatBoxDataToClient.handleOnClient(packet)));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.SyncEntityData.TYPE, ((packet, context) -> ChatBoxPayload.SyncEntityData.handleOnClient(packet)));

        ClientPlayNetworking.registerGlobalReceiver(SimplePayload.TYPE, ((packet, context) -> SimplePayload.handleOnClient(packet)));
    }

    private void registerRenderEvents() {
        HudElementRegistry.addLast(ChatBox.id("chatbox_hud"), ChatBoxRender::onHudRender);
        ClientTickEvents.END_CLIENT_TICK.register(ChatBoxRender::onEndTick);
        InputEvent.KEY.register(ChatBoxRender::onKey);
        InputEvent.MouseButton.POST.register(ChatBoxRender::mousePost);
        InputEvent.MOUSE_SCROLLING.register(ChatBoxRender::onMouseScroll);
    }
}
