package com.zhenshiz.chatbox.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ClientChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.mvel.MVELUtil;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class ChatBoxCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
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
                        .then(Commands.argument("Dialogues", ResourceLocationArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxDialoguesLoader.dialoguesMap.keySet(), builder))
                                .then(Commands.argument("MaxTriggerCount", IntegerArgumentType.integer())
                                        .executes(ChatBoxCommand::setMaxTriggerCount)
                                )
                        )
                        .then(Commands.literal("reset")
                                .executes(ChatBoxCommand::resetMaxTriggerCount)
                        )
                )
                .then(Commands.literal("command")
                        .then(Commands.literal("nextDialogue")
                                .executes(ChatBoxCommand::nextDialogue)
                        )
                        .then(Commands.literal("autoPlay")
                                .then(Commands.argument("AutoPlay", BoolArgumentType.bool())
                                        .executes(ChatBoxCommand::autoPlay)
                                )
                        )
                        .then(Commands.literal("isScreen")
                                .then(Commands.argument("IsScreen", BoolArgumentType.bool())
                                        .executes(ChatBoxCommand::toggleIsScreen)
                                )
                        )
                )
                .then(Commands.literal("mvelTest")
                        .then(Commands.argument("expression", StringArgumentType.string())
                                .then(Commands.argument("onServer", BoolArgumentType.bool())
                                        .executes(context -> {
                                            String expression = StringArgumentType.getString(context, "expression");
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) {
                                                context.getSource().sendFailure(ERROR_PLAYER_ONLY);
                                                return 0;
                                            }
                                            if (BoolArgumentType.getBool(context, "onServer")) {
                                                MVELUtil.commandTest(player, expression);
                                            } else SimplePayload.simplePayloadS2C(player, SimplePayload.MVEL_TEST, expression);
                                            return 1;
                                        })
                                )
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
            } catch (CommandSyntaxException ignored) {
            }
            // 防止重复添加目标
            if (entity != null && !entities.contains(entity)) entities.add(entity);
        }
        return entities;
    }

    private static int toggleIsScreen(CommandContext<CommandSourceStack> context) {
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
        ResourceLocation theme = ResourceLocationArgument.getId(context, "Theme");
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
        ResourceLocation dialogues = ResourceLocationArgument.getId(context, "Dialogues");
        String group = StringArgumentType.getString(context, "Group");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            ChatBoxCommandUtil.serverSkipDialogues(player, dialogues, group, index, targets);
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
            ResourceLocation dialogues = ResourceLocationArgument.getId(context, "Dialogues");
            int maxTriggerCount = IntegerArgumentType.getInteger(context, "MaxTriggerCount");
            ChatBoxCommandUtil.serverSetMaxTriggerCount(player, dialogues, maxTriggerCount);
            player.connection.send(new ClientChatBoxPayload.SetMaxTriggerCount(dialogues, maxTriggerCount));
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
            ChatBoxCommandUtil.serverResetMaxTriggerCount(player);
            player.connection.send(new ClientChatBoxPayload.ResetMaxTriggerCount());
            context.getSource().sendSuccess(() -> Component.translatable("commands.reset.max.trigger.count"), true);
            return 1;
        } else {
            context.getSource().sendFailure(ERROR_PLAYER_ONLY);
            return 0;
        }
    }
}
