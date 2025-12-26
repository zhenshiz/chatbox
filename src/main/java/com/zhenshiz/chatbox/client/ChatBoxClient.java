package com.zhenshiz.chatbox.client;

import com.zhenshiz.chatbox.Config;
import com.zhenshiz.chatbox.compat.plugin.PluginHelper;
import com.zhenshiz.chatbox.event.fabric.InputEvent;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.render.ChatBoxRender;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class ChatBoxClient implements ClientModInitializer {
    public static Config conf;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(Config.class, Toml4jConfigSerializer::new);
        conf = AutoConfig.getConfigHolder(Config.class).getConfig();
        registerReceiver();
        registerRenderEvents();
        PluginHelper.init();
    }

    private static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.OpenScreen.TYPE, ChatBoxPayload.OpenScreen::execute);

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.AllChatBoxThemeToClient.TYPE, ChatBoxPayload.AllChatBoxThemeToClient::execute);

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.AllChatBoxDialoguesToClient.TYPE, ChatBoxPayload.AllChatBoxDialoguesToClient::execute);

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.SyncEntityData.TYPE, ChatBoxPayload.SyncEntityData::execute);

        ClientPlayNetworking.registerGlobalReceiver(SimplePayload.TYPE, SimplePayload::execute);
    }

    private void registerRenderEvents() {
        HudRenderCallback.EVENT.register(new ChatBoxRender());
        ClientTickEvents.END_CLIENT_TICK.register(new ChatBoxRender());
        InputEvent.KEY.register(new ChatBoxRender());
        InputEvent.MouseButton.POST.register(new ChatBoxRender());
        InputEvent.MOUSE_SCROLLING.register(new ChatBoxRender());
    }
}
