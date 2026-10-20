package com.zhenshiz.chatbox.editor;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
//? fabric {
/*import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//? >= 1.21 {
/^import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
^///?} else {
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
//?}
*///?} else {
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
//?}

/** 提供独立、可检测的编辑器通道，未安装网页桥接的服务端仍可正常连接游戏。 */
public final class EditorNetwork {
    //? forge {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ChatBox.id("editor"), () -> "1", EditorNetwork::accepts, EditorNetwork::accepts);

    private static boolean accepts(String version) {
        return version.equals("1") || version.equals(NetworkRegistry.ABSENT) || version.equals(NetworkRegistry.ACCEPTVANILLA);
    }
    //?}

    private EditorNetwork() { }

    /** 注册网络类型与服务端接收器，只在模组初始化时调用一次。 */
    public static void register() {
        //? fabric {
        /*//? >= 1.21 {
        /^PayloadTypeRegistry.playC2S().register(EditorFilePayload.TYPE, EditorFilePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EditorFilePayload.TYPE, EditorFilePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EditorFilePayload.TYPE,
                (packet, context) -> EditorFilePayload.handleOnServer(context.player(), packet));
        ^///?} else {
        ServerPlayNetworking.registerGlobalReceiver(EditorFilePayload.ID,
                (server, player, handler, buffer, sender) -> EditorFilePayload.handleOnServer(player, EditorFilePayload.decode(buffer)));
        //?}
        *///?} else {
        CHANNEL.registerMessage(0, EditorFilePayload.class, EditorFilePayload::encode, EditorFilePayload::decode, (packet, context) -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) context.get().enqueueWork(() -> EditorFilePayload.handleOnClient(packet));
            else context.get().enqueueWork(() -> EditorFilePayload.handleOnServer(player, packet));
            context.get().setPacketHandled(true);
        });
        //?}
    }

    /** 注册 Fabric 客户端响应接收器；Forge 由双向通道分派。 */
    public static void registerClient() {
        //? fabric {
        /*//? >= 1.21 {
        /^ClientPlayNetworking.registerGlobalReceiver(EditorFilePayload.TYPE,
                (packet, context) -> EditorFilePayload.handleOnClient(packet));
        ^///?} else {
        ClientPlayNetworking.registerGlobalReceiver(EditorFilePayload.ID,
                (client, handler, buffer, sender) -> EditorFilePayload.handleOnClient(EditorFilePayload.decode(buffer)));
        //?}
        *///?}
    }

    /**
     * 判断当前服务端是否声明了编辑器协议。
     *
     * @return 当前连接包含编辑器通道时返回 {@code true}
     */
    public static boolean canSend() {
        if (Minecraft.getInstance().getConnection() == null) return false;
        //? fabric {
        /*//? >= 1.21 {
        /^return ClientPlayNetworking.canSend(EditorFilePayload.TYPE);
        ^///?} else {
        return ClientPlayNetworking.canSend(EditorFilePayload.ID);
        //?}
        *///?} else {
        return CHANNEL.isRemotePresent(Minecraft.getInstance().getConnection().getConnection());
        //?}
    }

    /**
     * 通过当前游戏连接发送请求分片。
     *
     * @param packet 已按协议限制大小的请求分片
     */
    public static void sendToServer(EditorFilePayload packet) {
        //? fabric {
        /*//? >= 1.21 {
        /^ClientPlayNetworking.send(packet);
        ^///?} else {
        var buffer = PacketByteBufs.create();
        packet.write(buffer);
        ClientPlayNetworking.send(packet.id(), buffer);
        //?}
        *///?} else {
        CHANNEL.sendToServer(packet);
        //?}
    }

    /**
     * 仅向原请求玩家返回响应分片。
     *
     * @param player 请求所属的已认证玩家
     * @param packet 响应分片
     */
    public static void sendToClient(ServerPlayer player, EditorFilePayload packet) {
        //? fabric {
        /*//? >= 1.21 {
        /^ServerPlayNetworking.send(player, packet);
        ^///?} else {
        var buffer = PacketByteBufs.create();
        packet.write(buffer);
        ServerPlayNetworking.send(player, packet.id(), buffer);
        //?}
        *///?} else {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        //?}
    }
}
