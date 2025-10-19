package com.zhenshiz.chatbox.api;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**选项点击事件，用于在对话中的选项被点击时触发自定义操作*/
public interface ChatOptionClickEvent {
    Map<String, ChatOptionClickEvent> CLICK_EVENTS = new HashMap<>();

    /**注册一个选项点击事件，必须要在mod主类中被调用才能生效*/
    static void registerClickEvent(ChatOptionClickEvent event) {
        CLICK_EVENTS.put(event.getType().toUpperCase(), event);
    }

    /**
     * 注册一个选项点击事件，必须要在mod主类中被调用才能生效
     * @param type                  点击事件类型id，英文字母无视大小写
     * @param executeOnClient       选项点击事件触发时，在客户端执行的操作
     * @param shouldExecuteOnServer 选项点击事件触发时，是否需要在服务端执行操作
     * @param executeOnServer       选项点击事件触发时，在服务端执行的操作
     */
    static void registerClickEvent(String type, Consumer<String> executeOnClient, Supplier<Boolean> shouldExecuteOnServer, BiConsumer<ServerPlayer, String> executeOnServer) {
        registerClickEvent(new ChatOptionClickEvent() {
            public String getType() {return type;}
            public void executeOnClient(String value) {executeOnClient.accept(value);}
            public boolean shouldExecuteOnServer() {return shouldExecuteOnServer.get();}
            public void executeOnServer(ServerPlayer player, String value) {executeOnServer.accept(player, value);}
        });
    }

    /**@return 点击事件类型id，英文字母无视大小写*/
    String getType();

    /**选项点击事件触发时，在客户端执行的操作*/
    void executeOnClient(String value);

    /**
     * 选项点击事件触发时，是否需要在服务端执行操作
     * @return true，会发包给服务端，然后{@link #executeOnServer(ServerPlayer, String)}的代码会在服务端执行
     */
    boolean shouldExecuteOnServer();

    /**选项点击事件触发时，在服务端执行的操作*/
    void executeOnServer(ServerPlayer player, String value);

     /**默认操作，若无必要请勿重写*/
    default void execute(String value) {
        executeOnClient(value);
        if (shouldExecuteOnServer()) {
            ChatBox.PLATFORM.sendToServer(new SendClickEvent(getType().toUpperCase(), value));
        }
    }

    class Command implements ChatOptionClickEvent {
        @Override public String getType() {return "COMMAND";}

        @Override public void executeOnClient(String value) {}

        @Override public boolean shouldExecuteOnServer() {return true;}

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
}
