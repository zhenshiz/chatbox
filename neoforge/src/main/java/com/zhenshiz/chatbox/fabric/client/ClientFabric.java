package com.zhenshiz.chatbox.fabric.client;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.client.ChatBoxRender;
import com.zhenshiz.chatbox.editor.EditorBridgeClient;
import com.zhenshiz.chatbox.editor.EditorFilePayload;
import com.zhenshiz.chatbox.fabric.event.InputEvent;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ChatBoxClient.init();
        registerClientHandlers();
        registerRenderEvents();
        registerEditorEvents();
    }

    public static void registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.ChatBoxDataToClient.TYPE, (packet, _) -> ChatBoxPayload.ChatBoxDataToClient.handleOnClient(packet));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.SyncEntityData.TYPE, (packet, _) -> ChatBoxPayload.SyncEntityData.handleOnClient(packet));

        ClientPlayNetworking.registerGlobalReceiver(SimplePayload.TYPE, (packet, _) -> SimplePayload.handleOnClient(packet));

        ClientPlayNetworking.registerGlobalReceiver(EditorFilePayload.TYPE, (packet, _) -> EditorFilePayload.handleOnClient(packet));
    }

    private void registerRenderEvents() {
        HudElementRegistry.addLast(ChatBox.id("chatbox_hud"), ChatBoxRender::onHudRender);
        ClientTickEvents.END_CLIENT_TICK.register(ChatBoxRender::onEndTick);
        InputEvent.KEY.register(ChatBoxRender::onKey);
        InputEvent.MouseButton.POST.register(ChatBoxRender::mousePost);
        InputEvent.MOUSE_SCROLLING.register(ChatBoxRender::onMouseScroll);
    }

    private void registerEditorEvents() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, _, _) -> EditorBridgeClient.commands(dispatcher)));
        ClientTickEvents.END_CLIENT_TICK.register(_ -> EditorBridgeClient.tick());
        ClientPlayConnectionEvents.DISCONNECT.register(((_, _) -> EditorBridgeClient.logout()));
    }
}
