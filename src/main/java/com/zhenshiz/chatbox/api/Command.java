package com.zhenshiz.chatbox.api;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
        CommandSourceStack commandSource;
        if (entity != null) commandSource = entity.createCommandSourceStack();
        else commandSource = server.createCommandSourceStack();
        commandSource = commandSource.withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();
        var dispatcher = server.getCommands().getDispatcher();
        try {
            dispatcher.execute(dispatcher.parse(command, commandSource));
        } catch (CommandSyntaxException e) {
            ChatBox.LOGGER.error("Error executing command on server: {}", command, e);
        }
    }
}
