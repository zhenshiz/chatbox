package com.zhenshiz.chatbox.client;

import com.zhenshiz.chatbox.event.fabric.InputEvent;
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
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.OpenScreen.ID, (client, h, buf, r) -> ChatBoxPayload.OpenScreen.handleOnClient(ChatBoxPayload.OpenScreen.decode(buf)));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.OpenChatBox.ID, (client, h, buf, r) -> ChatBoxPayload.OpenChatBox.handleOnClient(ChatBoxPayload.OpenChatBox.decode(buf)));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.ToggleTheme.ID, (client, h, buf, r) -> ChatBoxPayload.ToggleTheme.handleOnClient(ChatBoxPayload.ToggleTheme.decode(buf)));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.AllChatBoxThemeToClient.ID, (client, h, buf, r) -> ChatBoxPayload.AllChatBoxThemeToClient.handleOnClient(ChatBoxPayload.AllChatBoxThemeToClient.decode(buf)));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.AllChatBoxDialoguesToClient.ID, (client, h, buf, r) -> ChatBoxPayload.AllChatBoxDialoguesToClient.handleOnClient(ChatBoxPayload.AllChatBoxDialoguesToClient.decode(buf)));
    }

    private void registerRenderEvents() {
        HudRenderCallback.EVENT.register(ChatBoxRenderCommon::onHudRender);
        ClientTickEvents.END_CLIENT_TICK.register(ChatBoxRenderCommon::onEndTick);
        InputEvent.KEY.register(ChatBoxRenderCommon::onKey);
        InputEvent.MouseButton.POST.register(ChatBoxRenderCommon::mousePost);
        InputEvent.MOUSE_SCROLLING.register(ChatBoxRenderCommon::onMouseScroll);
    }
}
