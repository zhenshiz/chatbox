package com.zhenshiz.chatbox.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ChatBoxCommand {
    public static final Component ERROR_ENTITY_ONLY = Component.translatable("command.target.entity.only");
    public static final Component ERROR_PLAYER_ONLY = Component.translatable("command.target.player.only");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal("chatbox").requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("theme")
                        .then(Commands.argument("Theme", ResourceLocationArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxThemeLoader.themeMap.keySet(), builder))
                                .executes(ChatBoxCommand::toggleTheme)
                        )
                )
                .then(Commands.literal("skip")
                        .then(Commands.argument("Dialogues", ResourceLocationArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxDialoguesLoader.dialoguesMap.keySet(), builder))
                                .then(Commands.argument("Group", StringArgumentType.string())
                                        .suggests(((context, builder) -> {
                                            ResourceLocation dialogues = ResourceLocationArgument.getId(context, "Dialogues");
                                            ChatBoxDialoguesLoader.dialoguesGroupMap.get(dialogues).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        }))
                                        .executes(context -> ChatBoxCommand.skipDialogues(context, 0))
                                        .then(Commands.argument("Index", IntegerArgumentType.integer())
                                                .executes(context -> ChatBoxCommand.skipDialogues(context, IntegerArgumentType.getInteger(context, "Index")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("open")
                        .executes(ChatBoxCommand::openChatBox)
                )
                .then(Commands.literal("maxTriggerCount")
                        .then(Commands.argument("Dialogues", ResourceLocationArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxDialoguesLoader.dialoguesMap.keySet(), builder))
                                .then(Commands.argument("MaxTriggerCount", IntegerArgumentType.integer())
                                        .executes(ChatBoxCommand::setMaxTriggerCount)
                                )
                        )
                        .then(Commands.literal("reset")
                                .executes(ChatBoxCommand::resetMaxTriggerCount)
                        )
                )
        );
    }

    private static int toggleTheme(CommandContext<CommandSourceStack> context) {
        ResourceLocation theme = ResourceLocationArgument.getId(context, "Theme");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.ToggleTheme(theme));
            context.getSource().sendSuccess(() -> Component.translatable("commands.toggle.theme"), true);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    private static int skipDialogues(CommandContext<CommandSourceStack> context, int index) {
        ResourceLocation dialogues = ResourceLocationArgument.getId(context, "Dialogues");
        String group = StringArgumentType.getString(context, "Group");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            //判断玩家的触发次数是否为0，为0则不触发对话
            ChatBoxTriggerCount counts = ChatBox.getTriggerCounts();
            int count = counts.getPlayerMaxTriggerCount(player, dialogues);
            if (count != 0) {
                counts.setPlayerMaxTriggerCount(player, dialogues, count - 1);
                ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.OpenScreen(dialogues, group, index));
                context.getSource().sendSuccess(() -> Component.translatable("commands.skip.dialogues", group, index + 1), true);
            }
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    private static int openChatBox(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.OpenChatBox());
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    private static int setMaxTriggerCount(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            ResourceLocation dialogues = ResourceLocationArgument.getId(context, "Dialogues");
            int maxTriggerCount = IntegerArgumentType.getInteger(context, "MaxTriggerCount");
            ChatBox.getTriggerCounts().setPlayerMaxTriggerCount(player, dialogues, maxTriggerCount);
            context.getSource().sendSuccess(() -> Component.translatable("commands.set.max.trigger.count", dialogues.toString(), maxTriggerCount), true);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    private static int resetMaxTriggerCount(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            ChatBox.getTriggerCounts().resetPlayerMaxTriggerCount(player);
            context.getSource().sendSuccess(() -> Component.translatable("commands.reset.max.trigger.count"), true);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }
}
