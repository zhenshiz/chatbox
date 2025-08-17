package com.zhenshiz.chatbox.network.c2s;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.network.CustomPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record SendCommandPayload(String command) implements CustomPacket {
    public ResourceLocation id() {return ID;}
    public static final ResourceLocation ID = ChatBox.ResourceLocationMod("execute_server_command");

    public void write(FriendlyByteBuf buf) {encode(this, buf);}

    public static void encode(SendCommandPayload packet, FriendlyByteBuf buf) {buf.writeUtf(packet.command);}

    public static SendCommandPayload decode(FriendlyByteBuf buf) {return new SendCommandPayload(buf.readUtf());}

    public static void handleOnServer(MinecraftServer server, ServerPlayer player, SendCommandPayload packet) {
        // 以玩家身份创建命令源，并赋予2级权限，且禁止输出
        CommandSourceStack commandSource = player.createCommandSourceStack()
                .withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();

        var command = packet.command;
        try {
            player.server.getCommands().performPrefixedCommand(commandSource, command);
        } catch (Exception e) {
            ChatBox.LOGGER.error("Error executing command on server: {}", command, e);
        }
    }
}
