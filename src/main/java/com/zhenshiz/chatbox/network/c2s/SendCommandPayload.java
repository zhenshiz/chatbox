package com.zhenshiz.chatbox.network.c2s;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SendCommandPayload(String command) implements CustomPacketPayload {
    public static final Type<SendCommandPayload> TYPE = new Type<>(ChatBox.ResourceLocationMod("execute_server_command"));
    public static final StreamCodec<FriendlyByteBuf, SendCommandPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SendCommandPayload::command,
            SendCommandPayload::new
    );

    public static void execute(SendCommandPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        String command = payload.command();

        CommandSourceStack commandSource = player.createCommandSourceStack()
                .withPermission(Commands.LEVEL_GAMEMASTERS)
                .withSuppressedOutput();

        try {
            player.server.getCommands().performPrefixedCommand(commandSource, command);
        } catch (Exception e) {
            ChatBox.LOGGER.error("Error executing command on server: {}", command, e);
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
