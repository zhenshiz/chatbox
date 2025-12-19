package com.zhenshiz.chatbox.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class ChatBoxCommand {
    public static final Component ERROR_ENTITY_ONLY = Component.translatable("command.target.entity.only");
    public static final Component ERROR_PLAYER_ONLY = Component.translatable("command.target.player.only");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal("chatbox").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("theme")
                        .then(Commands.argument("Theme", IdentifierArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxThemeLoader.themeMap.keySet(), builder))
                                .executes(ChatBoxCommand::toggleTheme)
                        )
                )
                .then(Commands.literal("skip")
                        .then(Commands.argument("Dialogues", IdentifierArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxDialoguesLoader.dialoguesMap.keySet(), builder))
                                .then(Commands.argument("Group", StringArgumentType.string())
                                        .suggests(((context, builder) -> {
                                            Identifier dialogues = IdentifierArgument.getId(context, "Dialogues");
                                            ChatBoxDialoguesLoader.dialoguesGroupMap.getOrDefault(dialogues, new HashSet<>()).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        }))
                                        .executes(context -> ChatBoxCommand.skipDialogues(context, 0))
                                        .then(Commands.argument("Index", IntegerArgumentType.integer())
                                                .executes(context -> ChatBoxCommand.skipDialogues(context, IntegerArgumentType.getInteger(context, "Index")))
                                                .then(buildTargets(1, 100))
                                        )
                                )
                        )
                )
                .then(Commands.literal("open")
                        .executes(ChatBoxCommand::openChatBox)
                )
                .then(Commands.literal("maxTriggerCount")
                        .then(Commands.argument("Dialogues", IdentifierArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxDialoguesLoader.dialoguesMap.keySet(), builder))
                                .then(Commands.argument("MaxTriggerCount", IntegerArgumentType.integer())
                                        .executes(ChatBoxCommand::setMaxTriggerCount)
                                )
                        )
                        .then(Commands.literal("reset")
                                .executes(ChatBoxCommand::resetMaxTriggerCount)
                        )
                )
                .then(Commands.literal("command")
                        .then(Commands.literal("nextDialogue").executes(ChatBoxCommand::nextDialogue))
                        .then(Commands.literal("autoPlay")
                                .then(Commands.argument("AutoPlay", BoolArgumentType.bool()).executes(ChatBoxCommand::autoPlay))
                        )
                        .then(Commands.literal("isScreen")
                                .then(Commands.argument("IsScreen", BoolArgumentType.bool()).executes(ChatBoxCommand::setIsScreen))
                        )
                )
        );
    }

    private static RequiredArgumentBuilder<CommandSourceStack, EntitySelector> buildTargets(int i, int max) {
        var builder = Commands.argument("Target" + i, EntityArgument.entity())
                .executes(context -> skipDialogues(context, IntegerArgumentType.getInteger(context, "Index"), getTargets(context, i)));
        if (i <= max) builder.then(buildTargets(i + 1, max));
        return builder;
    }

    private static List<Entity> getTargets(CommandContext<CommandSourceStack> context, int num) {
        List<Entity> entities = new ArrayList<>();
        for (int i = 1; i <= num; i++) { // 注意这个是从1开始
            Entity entity = null;
            try {
                entity = EntityArgument.getEntity(context, "Target" + i);
            } catch (CommandSyntaxException ignored) {}
            // 防止重复添加目标
            if (entity != null && !entities.contains(entity)) entities.add(entity);
        }
        return entities;
    }

    private static int setIsScreen(CommandContext<CommandSourceStack> context) {
        boolean isScreen = BoolArgumentType.getBool(context, "IsScreen");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            ChatBoxCommandUtil.serverSetIsScreen(player, isScreen);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    private static int autoPlay(CommandContext<CommandSourceStack> context) {
        boolean autoPlay = BoolArgumentType.getBool(context, "AutoPlay");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            ChatBoxCommandUtil.serverAutoPlay(player, autoPlay);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    private static int nextDialogue(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            ChatBoxCommandUtil.serverNextDialogue(player);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    private static int toggleTheme(CommandContext<CommandSourceStack> context) {
        Identifier theme = IdentifierArgument.getId(context, "Theme");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            ChatBoxCommandUtil.serverToggleTheme(player, theme);
            context.getSource().sendSuccess(() -> Component.translatable("commands.toggle.theme"), true);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    public static final Map<UUID, List<Entity>> TARGETS_MAP = new HashMap<>();

    private static int skipDialogues(CommandContext<CommandSourceStack> context, int index) {
        return skipDialogues(context, index, List.of());
    }

    private static int skipDialogues(CommandContext<CommandSourceStack> context, int index, List<Entity> targets) {
        Identifier dialogues = IdentifierArgument.getId(context, "Dialogues");
        String group = StringArgumentType.getString(context, "Group");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            //判断玩家的触发次数是否为0，为0则不触发对话
            int count = ChatBoxCommandUtil.serverGetMaxTriggerCount(player, dialogues);
            if (count != 0) {
                ChatBoxCommandUtil.serverSetMaxTriggerCount(player, dialogues, count - 1);
                ChatBoxCommandUtil.serverSkipDialogues(player, dialogues, group, index, targets);
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
            ChatBoxCommandUtil.serverOpenChatBox(player);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }

    private static int setMaxTriggerCount(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            Identifier dialogues = IdentifierArgument.getId(context, "Dialogues");
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
