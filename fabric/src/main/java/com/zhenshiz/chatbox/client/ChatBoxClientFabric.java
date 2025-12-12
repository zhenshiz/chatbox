package com.zhenshiz.chatbox.client;

import com.zhenshiz.chatbox.event.fabric.InputEvent;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.render.ChatBoxRenderCommon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class ChatBoxClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ChatBoxClient.init();
        registerClientHandlers();
        registerRenderEvents();
    }

    public static void registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.OpenScreen.TYPE, ((packet, context) -> ChatBoxPayload.OpenScreen.handleOnClient(packet)));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.AllChatBoxThemeToClient.TYPE, ((packet, context) -> ChatBoxPayload.AllChatBoxThemeToClient.handleOnClient(packet)));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.AllChatBoxDialoguesToClient.TYPE, ((packet, context) -> ChatBoxPayload.AllChatBoxDialoguesToClient.handleOnClient(packet)));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.SyncEntityData.TYPE, ((packet, context) -> ChatBoxPayload.SyncEntityData.handleOnClient(packet)));

        ClientPlayNetworking.registerGlobalReceiver(SimplePayload.TYPE, ((packet, context) -> SimplePayload.handleOnClient(packet)));
    }

    private void registerRenderEvents() {
        HudRenderCallback.EVENT.register(ChatBoxRenderCommon::onHudRender);
        ClientTickEvents.END_CLIENT_TICK.register(ChatBoxRenderCommon::onEndTick);
        InputEvent.KEY.register(ChatBoxRenderCommon::onKey);
        InputEvent.MouseButton.POST.register(ChatBoxRenderCommon::mousePost);
        InputEvent.MOUSE_SCROLLING.register(ChatBoxRenderCommon::onMouseScroll);
    }
}
