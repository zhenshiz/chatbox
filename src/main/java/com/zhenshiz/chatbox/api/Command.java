package com.zhenshiz.chatbox.api;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Command implements ChatOptionClickEvent {
    @Override
    public String getType() {
        return "COMMAND";
    }

    @Override
    public void executeOnClient(String value) {
    }

    @Override
    public boolean shouldExecuteOnServer() {
        return true;
    }

    @Override
    public void executeOnServer(ServerPlayer player, String value) {
        var commands = value.split(";");
        for (var command : commands) {
            command = command.trim();
            if (!command.isBlank()) executeCommand(player.server, player, command);
        }
    }

    public static void executeCommand(@NotNull MinecraftServer server, @Nullable Entity entity, String command) {
        // 创建命令源，并赋予2级权限，且禁止输出
        CommandSourceStack commandSource = server.createCommandSourceStack()
                .withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();
        if (entity != null) commandSource = commandSource.withEntity(entity);
        try {
            server.getCommands().performPrefixedCommand(commandSource, command);
        } catch (Exception e) {
            ChatBox.LOGGER.error("Error executing command on server: {}", command, e);
        }
    }
}
