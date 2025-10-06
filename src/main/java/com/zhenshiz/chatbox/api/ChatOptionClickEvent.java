package com.zhenshiz.chatbox.api;

import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 选项点击事件，用于在对话中的选项被点击时触发自定义操作
 */
public interface ChatOptionClickEvent {
    Map<String, ChatOptionClickEvent> CLICK_EVENTS = new HashMap<>();

    /**
     * 注册一个选项点击事件，必须要在mod主类中被调用才能生效
     */
    static void registerClickEvent(ChatOptionClickEvent event) {
        CLICK_EVENTS.put(event.getType(), event);
    }

    /**
     * 注册一个选项点击事件，必须要在mod主类中被调用才能生效
     *
     * @param type                  点击事件类型id，英文字母必须全部大写
     * @param executeOnClient       选项点击事件触发时，在客户端执行的操作
     * @param shouldExecuteOnServer 选项点击事件触发时，是否需要在服务端执行操作
     * @param executeOnServer       选项点击事件触发时，在服务端执行的操作
     */
    static void registerClickEvent(String type, Consumer<String> executeOnClient, Boolean shouldExecuteOnServer, BiConsumer<ServerPlayer, String> executeOnServer) {
        registerClickEvent(new ChatOptionClickEvent() {
            public String getType() {
                return type;
            }

            public void executeOnClient(String value) {
                executeOnClient.accept(value);
            }

            public boolean shouldExecuteOnServer() {
                return shouldExecuteOnServer;
            }

            public void executeOnServer(ServerPlayer player, String value) {
                executeOnServer.accept(player, value);
            }
        });
    }

    /**
     * @return 点击事件类型id，英文字母必须全部大写
     */
    String getType();

    /**
     * 选项点击事件触发时，在客户端执行的操作
     */
    void executeOnClient(String value);

    /**
     * 选项点击事件触发时，是否需要在服务端执行操作
     *
     * @return true，会发包给服务端，然后{@link #executeOnServer(ServerPlayer, String)}的代码会在服务端执行
     */
    boolean shouldExecuteOnServer();

    /**
     * 选项点击事件触发时，在服务端执行的操作
     */
    void executeOnServer(ServerPlayer player, String value);

    /**
     * 默认操作，若无必要请勿重写
     */
    default void execute(String value) {
        executeOnClient(value);
        var player = Minecraft.getInstance().player;
        if (shouldExecuteOnServer() && player != null) {
            player.connection.send(new SendClickEvent(getType(), value));
        }
    }
}
