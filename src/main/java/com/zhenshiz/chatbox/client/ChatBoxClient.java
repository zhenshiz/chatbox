package com.zhenshiz.chatbox.client;

import com.zhenshiz.chatbox.Config;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public class ChatBoxClient implements ClientModInitializer {
    public static Config conf;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(Config.class, Toml4jConfigSerializer::new);
        conf = AutoConfig.getConfigHolder(Config.class).getConfig();
        registerReceiver();
    }

    private static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.OpenScreenPayload.TYPE,
                (payload, context) -> ChatBoxUtil.skipDialogues(payload.dialogues(), payload.group(), payload.index()));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.OpenChatBox.TYPE,
                (payload, context) -> ChatBoxCommandUtil.clientOpenChatBox());

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.ToggleTheme.TYPE,
                (payload, context) -> ChatBoxCommandUtil.clientToggleTheme(payload.theme()));

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.AllChatBoxThemeToClient.TYPE,
                (payload, context) -> {
                    ChatBoxUtil.setTheme(payload.themeMap());
                    if (ChatBoxCommandUtil.themeResourceLocation != null) {
                        ResourceLocation theme = ResourceLocation.tryParse(ChatBoxCommandUtil.themeResourceLocation);
                        if (theme != null) {
                            ChatBoxUtil.toggleTheme(theme);
                        }
                    }
                });

        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.AllChatBoxDialoguesToClient.TYPE,
                (payload, context) -> ChatBoxUtil.setDialogues(payload.dialoguesMap()));
    }
}
