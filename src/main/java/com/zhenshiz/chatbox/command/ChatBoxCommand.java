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

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ChatBoxCommand {
    public static final Component ERROR_ENTITY_ONLY = Component.translatable("command.target.entity.only");
    public static final Component ERROR_PLAYER_ONLY = Component.translatable("command.target.player.only");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(literal("chatbox").requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(literal("theme")
                        .then(argument("Theme", ResourceLocationArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxThemeLoader.themeMap.keySet(), builder))
                                .executes(ChatBoxCommand::toggleTheme)
                        )
                )
                .then(literal("skip")
                        .then(argument("Dialogues", ResourceLocationArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxDialoguesLoader.dialoguesMap.keySet(), builder))
                                .then(argument("Group", StringArgumentType.string())
                                        .suggests(((context, builder) -> {
                                            ResourceLocation dialogues = ResourceLocationArgument.getId(context, "Dialogues");
                                            ChatBoxDialoguesLoader.dialoguesGroupMap.getOrDefault(dialogues, new HashSet<>()).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        }))
                                        .executes(context -> ChatBoxCommand.skipDialogues(context, 0))
                                        .then(argument("Index", IntegerArgumentType.integer())
                                                .executes(context -> ChatBoxCommand.skipDialogues(context, IntegerArgumentType.getInteger(context, "Index")))
                                                .then(buildTargets(1, 100))
                                        )
                                )
                        )
                )
                .then(literal("open")
                        .executes(ChatBoxCommand::openChatBox)
                )
                .then(literal("maxTriggerCount")
                        .then(argument("Dialogues", ResourceLocationArgument.id()).suggests((context, builder) -> SharedSuggestionProvider.suggestResource(ChatBoxDialoguesLoader.dialoguesMap.keySet(), builder))
                                .then(argument("MaxTriggerCount", IntegerArgumentType.integer())
                                        .executes(ChatBoxCommand::setMaxTriggerCount)
                                )
                        )
                        .then(literal("reset")
                                .executes(ChatBoxCommand::resetMaxTriggerCount)
                        )
                )
                .then(literal("command")
                        .then(literal("nextDialogue").executes(ChatBoxCommand::nextDialogue)
                                .then(argument("Delta", IntegerArgumentType.integer()).executes(ChatBoxCommand::nextDialogue)))
                        .then(literal("autoPlay")
                                .then(argument("AutoPlay", BoolArgumentType.bool()).executes(ChatBoxCommand::autoPlay))
                        )
                        .then(literal("isScreen")
                                .then(argument("IsScreen", BoolArgumentType.bool()).executes(ChatBoxCommand::setIsScreen))
                        )
                )
                .then(literal("mvelTest")
                        .then(argument("expression", StringArgumentType.string())
                                .then(argument("onServer", BoolArgumentType.bool())
                                        .executes(context -> {
                                            String expression = StringArgumentType.getString(context, "expression");
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) return alertNoPlayer(context);
                                            if (BoolArgumentType.getBool(context, "onServer")) {
                                                MVELUtil.commandTest(player, expression);
                                            } else ChatBoxCommandUtil.simplePayloadS2C(player, "mvel_test", expression);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(literal("group")
                        .then(literal("add")
                                .then(argument("Leader", EntityArgument.player())
                                        .then(argument("Members", EntityArgument.players()).executes(ChatBoxCommand::addGroup))
                                )
                        )
                        .then(literal("remove")
                                .then(argument("Member", EntityArgument.player()).executes(ChatBoxCommand::removePlayerFromGroup))
                        )
                        .then(literal("setLeader")
                                .then(argument("NewLeader", EntityArgument.player()).executes(ChatBoxCommand::setLeader))
                        )
                        .then(literal("clear").executes(ChatBoxCommand::clearGroup))
                )
        );
    }

    private static RequiredArgumentBuilder<CommandSourceStack, EntitySelector> buildTargets(int i, int max) {
        var builder = argument("Target" + i, EntityArgument.entity())
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
        } else return alertNoPlayer(context);
    }

    private static int autoPlay(CommandContext<CommandSourceStack> context) {
        boolean autoPlay = BoolArgumentType.getBool(context, "AutoPlay");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            ChatBoxCommandUtil.serverAutoPlay(player, autoPlay);
            return 1;
        } else return alertNoPlayer(context);
    }

    private static int nextDialogue(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            Integer delta = null;
            try {
                delta = IntegerArgumentType.getInteger(context, "Delta");
            } catch (IllegalArgumentException ignored) {}
            ChatBoxCommandUtil.serverNextDialogue(player, delta);
            return 1;
        } else return alertNoPlayer(context);
    }

    private static int toggleTheme(CommandContext<CommandSourceStack> context) {
        ResourceLocation theme = ResourceLocationArgument.getId(context, "Theme");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            ChatBoxCommandUtil.serverToggleTheme(player, theme);
            context.getSource().sendSuccess(() -> Component.translatable("commands.toggle.theme"), true);
            return 1;
        } else return alertNoPlayer(context);
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
            //判断玩家的触发次数是否为0，为0则不触发对话
            int count = ChatBoxCommandUtil.serverGetMaxTriggerCount(player, dialogues);
            if (count != 0) {
                ChatBoxCommandUtil.serverSetMaxTriggerCount(player, dialogues, count - 1);
                ChatBoxCommandUtil.serverSkipDialogues(player, dialogues, group, index, targets);
                context.getSource().sendSuccess(() -> Component.translatable("commands.skip.dialogues", group, index + 1), true);
            }
            return 1;
        } else return alertNoPlayer(context);
    }

    private static int openChatBox(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            ChatBoxCommandUtil.serverOpenChatBox(player);
            return 1;
        } else return alertNoPlayer(context);
    }

    private static int setMaxTriggerCount(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            ResourceLocation dialogues = ResourceLocationArgument.getId(context, "Dialogues");
            int maxTriggerCount = IntegerArgumentType.getInteger(context, "MaxTriggerCount");
            ChatBox.getSavedData().setPlayerMaxTriggerCount(player, dialogues, maxTriggerCount);
            context.getSource().sendSuccess(() -> Component.translatable("commands.set.max.trigger.count", dialogues.toString(), maxTriggerCount), true);
            return 1;
        } else return alertNoPlayer(context);
    }

    private static int resetMaxTriggerCount(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            ChatBox.getSavedData().resetPlayerMaxTriggerCount(player);
            context.getSource().sendSuccess(() -> Component.translatable("commands.reset.max.trigger.count"), true);
            return 1;
        } else return alertNoPlayer(context);
    }

    private static int addGroup(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer leader = EntityArgument.getPlayer(context, "Leader");
            var members = EntityArgument.getPlayers(context, "Members");
            for (Component component : ChatBox.getSavedData().addPlayerToGroup(leader, members)) {
                context.getSource().sendSuccess(() -> component, false);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int removePlayerFromGroup(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer member = EntityArgument.getPlayer(context, "Member");
            context.getSource().sendSuccess(() -> ChatBox.getSavedData().removePlayerFromGroup(member), false);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setLeader(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer leader = EntityArgument.getPlayer(context, "NewLeader");
            context.getSource().sendSuccess(() -> ChatBox.getSavedData().setLeader(leader), false);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int clearGroup(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> ChatBox.getSavedData().clearGroup(), true);
        return 1;
    }

    private static int alertNoPlayer(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(ERROR_PLAYER_ONLY);
        return 0;
    }
}
