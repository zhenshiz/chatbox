package com.zhenshiz.chatbox.network.c2s;

import com.zhenshiz.chatbox.ChatBox;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public record SendCommandPayload(String command) implements CustomPacketPayload {
    public static final Type<SendCommandPayload> TYPE = new Type<>(ChatBox.ResourceLocationMod("execute_server_command"));
    public static final StreamCodec<FriendlyByteBuf, SendCommandPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SendCommandPayload::command,
            SendCommandPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void execute(SendCommandPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        // 以玩家身份创建命令源，并赋予2级权限，且禁止输出
        CommandSourceStack commandSource = player.createCommandSourceStack()
                .withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();

        var command = payload.command();
        try {
            player.server.getCommands().performPrefixedCommand(commandSource, command);
        } catch (Exception e) {
            ChatBox.LOGGER.error("Error executing command on server: {}", command, e);
        }
    }
}
