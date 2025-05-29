package com.zhenshiz.chatbox.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public interface ICommand {
    Component ERROR_ENTITY_ONLY = Component.translatable("command.target.entity.only");
    Component ERROR_PLAYER_ONLY = Component.translatable("command.target.player.only");

    void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection);
}
