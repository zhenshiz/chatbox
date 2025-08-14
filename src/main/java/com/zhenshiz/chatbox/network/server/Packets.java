package com.zhenshiz.chatbox.network.server;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.payload.c2s.SendCommandPayload;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class Packets {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.OpenScreenPayload.TYPE, ChatBoxPayload.OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.OpenChatBox.TYPE, ChatBoxPayload.OpenChatBox.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.ToggleTheme.TYPE, ChatBoxPayload.ToggleTheme.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.AllChatBoxThemeToClient.TYPE, ChatBoxPayload.AllChatBoxThemeToClient.CODEC);
        PayloadTypeRegistry.playS2C().register(ChatBoxPayload.AllChatBoxDialoguesToClient.TYPE, ChatBoxPayload.AllChatBoxDialoguesToClient.CODEC);
        PayloadTypeRegistry.playC2S().register(SendCommandPayload.TYPE, SendCommandPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SendCommandPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            // 以玩家身份创建命令源，并赋予2级权限，且禁止输出
            CommandSourceStack commandSource = player.createCommandSourceStack()
                    .withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();

            var command = payload.command();
            try {
                player.level().getServer().getCommands().performPrefixedCommand(commandSource, command);
            } catch (Exception e) {
                ChatBox.LOGGER.error("Error executing command on server: {}", command, e);
            }
        });
    }
}