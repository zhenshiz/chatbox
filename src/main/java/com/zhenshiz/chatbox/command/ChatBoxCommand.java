package com.zhenshiz.chatbox.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.payload.s2c.ChatBoxPayload;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ChatBoxCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal("chatbox").requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("theme")
                        .then(Commands.argument("Theme", ResourceLocationArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxThemeLoader.INSTANCE.themeMap.keySet(), builder))
                                .executes(ChatBoxCommand::toggleTheme)
                        )
                )
                .then(Commands.literal("skip")
                        .then(Commands.argument("Dialogues", ResourceLocationArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxDialoguesLoader.INSTANCE.dialoguesMap.keySet(), builder))
                                .then(Commands.argument("Group", StringArgumentType.string())
                                        .executes(context -> ChatBoxCommand.skipDialogues(context, true))
                                        .then(Commands.argument("Index", IntegerArgumentType.integer())
                                                .executes(context -> ChatBoxCommand.skipDialogues(context, false))
                                        )
                                )
                        )
                )
                .then(Commands.literal("open")
                        .executes(ChatBoxCommand::openChatBox)
                )
        );
    }

    private static int toggleTheme(CommandContext<CommandSourceStack> context) {
        ResourceLocation theme = ResourceLocationArgument.getId(context, "Theme");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            player.connection.send(new ChatBoxPayload.ToggleTheme(theme));
            context.getSource().sendSuccess(() -> Component.translatable("commands.toggle.theme"), true);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    private static int skipDialogues(CommandContext<CommandSourceStack> context, boolean isStart) {
        ResourceLocation dialogues = ResourceLocationArgument.getId(context, "Dialogues");
        String group = StringArgumentType.getString(context, "Group");
        int index = IntegerArgumentType.getInteger(context, "Index");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            if (isStart) {
                player.connection.send(new ChatBoxPayload.OpenScreenPayload(dialogues, group, 0));
            } else {
                player.connection.send(new ChatBoxPayload.OpenScreenPayload(dialogues, group, index));
            }
            context.getSource().sendSuccess(() -> Component.translatable("commands.skip.dialogues", group, index + 1), true);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    private static int openChatBox(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            player.connection.send(new ChatBoxPayload.OpenChatBox());

            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }
}
