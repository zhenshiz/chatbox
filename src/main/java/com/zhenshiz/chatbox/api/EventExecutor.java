package com.zhenshiz.chatbox.api;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.network.c2s.SendClickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**对话框组件事件的执行逻辑，用于在对话渲染中合适的时机触发自定义操作*/
public interface EventExecutor {
    Map<String, EventExecutor> EXECUTORS = new HashMap<>();

    /**注册一个组件事件，必须要在mod主类中被调用才能生效*/
    static void registerEvent(EventExecutor event) {
        EXECUTORS.put(event.getType().toUpperCase(), event);
    }

    /**
     * 注册一个组件事件，必须要在mod主类中被调用才能生效
     * @param type                  组件事件类型id，英文字母无视大小写
     * @param executeOnClient       组件事件触发时，在客户端执行的操作
     * @param shouldExecuteOnServer 组件事件触发时，是否需要在服务端执行操作
     * @param executeOnServer       组件事件触发时，在服务端执行的操作
     */
    static void registerEvent(String type, BiConsumer<AbstractComponent<?>, String> executeOnClient, Supplier<Boolean> shouldExecuteOnServer, BiConsumer<ServerPlayer, String> executeOnServer) {
        registerEvent(new EventExecutor() {
            public String getType() {return type;}
            public void executeOnClient(AbstractComponent<?> c, String v) {executeOnClient.accept(c, v);}
            public boolean shouldExecuteOnServer() {return shouldExecuteOnServer.get();}
            public void executeOnServer(ServerPlayer player, String value) {executeOnServer.accept(player, value);}
        });
    }

    /**
     * 注册一个组件事件，必须要在mod主类中被调用才能生效
     * @param type                  组件事件类型id，英文字母无视大小写
     * @param executeOnClient       组件事件触发时，在客户端执行的操作
     */
    static void registerClientEvent(String type, BiConsumer<AbstractComponent<?>, String> executeOnClient) {
        registerEvent(type, executeOnClient, () -> false, (player, value) -> {});
    }

    /**
     * 手动执行一个事件
     * @return 是否包含该事件类型，若没有已注册的事件类型，则返回false
     */
    static boolean executeEvent(AbstractComponent<?> component, String type, String value) {
        EventExecutor executor = EXECUTORS.getOrDefault(type.toUpperCase(), null);
        if (executor != null) {
            executor.execute(component, value == null ? "" : value);
            return true;
        } else {
            ChatBox.LOGGER.warn("Unknown event type: {}, available types: {}", type, EXECUTORS.keySet());
            return false;
        }
    }

    /**@return 组件事件类型id，英文字母无视大小写*/
    String getType();

    /**组件事件触发时，在客户端执行的操作*/
    void executeOnClient(AbstractComponent<?> component, String value);

    /**
     * 组件事件触发时，是否需要在服务端执行操作
     * @return true，会发包给服务端，然后{@link #executeOnServer(ServerPlayer, String)}的代码会在服务端执行
     */
    boolean shouldExecuteOnServer();

    /**组件事件触发时，在服务端执行的操作*/
    void executeOnServer(ServerPlayer player, String value);

     /**默认操作，若无必要请勿重写*/
    default void execute(AbstractComponent<?> component, String value) {
        executeOnClient(component, value);
        if (shouldExecuteOnServer()) {
            PacketDistributor.sendToServer(new SendClickEvent(getType().toUpperCase(), value));
        }
    }
}
