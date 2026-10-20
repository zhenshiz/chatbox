package com.zhenshiz.chatbox.editor;

import com.zhenshiz.chatbox.ChatBox;
//? fabric {
/*import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/** 将加载器的客户端命令和退出事件转交给共用编辑器桥接。 */
//? forge
@Mod.EventBusSubscriber(modid = ChatBox.MOD_ID, value = Dist.CLIENT)
public final class EditorClientEvents {
    private EditorClientEvents() { }

    /** 在客户端初始化时安装网络接收器及加载器回调。 */
    public static void register() {
        EditorNetwork.registerClient();
        //? fabric {
        /*ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> EditorBridgeClient.commands(dispatcher,
                (source, text) -> source.sendFeedback(text), (source, text) -> source.sendError(text)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EditorBridgeClient.logout());
        *///?}
    }

    //? forge {
    /**
     * 在 Forge 客户端命令注册时加入编辑器入口。
     *
     * @param event 提供客户端命令分派器的注册事件
     */
    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        EditorBridgeClient.commands(event.getDispatcher(),
                (source, text) -> source.sendSuccess(() -> text, false), (source, text) -> source.sendFailure(text));
    }

    /**
     * 在 Forge 游戏连接断开后释放编辑器会话。
     *
     * @param event 当前客户端的退出事件
     */
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        EditorBridgeClient.logout();
    }
    //?}
}
