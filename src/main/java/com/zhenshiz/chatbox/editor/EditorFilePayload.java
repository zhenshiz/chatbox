package com.zhenshiz.chatbox.editor;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
//? >= 1.21 {
/*import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
*///?} else {
import com.zhenshiz.chatbox.network.CustomPacket;
import net.minecraft.resources.ResourceLocation;
//?}

/**
 * 通过已认证的游戏连接传输编辑器文件请求或响应的受限分片。
 *
 * @param requestId 客户端生成的请求 UUID，最长 64 个字符
 * @param index 从 0 开始的分片索引
 * @param total 分片总数
 * @param json JSON 分片，最多 8000 个 UTF-16 代码单元
 */
public record EditorFilePayload(String requestId, int index, int total, String json) implements
//? >= 1.21 {
        /*CustomPacketPayload {
    public static final Type<EditorFilePayload> TYPE = new Type<>(ChatBox.id("editor_files"));
    public static final StreamCodec<FriendlyByteBuf, EditorFilePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), EditorFilePayload::requestId,
            ByteBufCodecs.VAR_INT, EditorFilePayload::index,
            ByteBufCodecs.VAR_INT, EditorFilePayload::total,
            ByteBufCodecs.stringUtf8(EditorFileChunks.CHUNK_SIZE * 3), EditorFilePayload::json,
            EditorFilePayload::new);

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
*///?} else {
        CustomPacket {
    public static final ResourceLocation ID = ChatBox.id("editor_files");

    @Override public ResourceLocation id() { return ID; }
    @Override public void write(FriendlyByteBuf buffer) { encode(this, buffer); }

    public static void encode(EditorFilePayload payload, FriendlyByteBuf buffer) {
        buffer.writeUtf(payload.requestId, 64);
        buffer.writeVarInt(payload.index);
        buffer.writeVarInt(payload.total);
        buffer.writeUtf(payload.json, EditorFileChunks.CHUNK_SIZE * 3);
    }

    public static EditorFilePayload decode(FriendlyByteBuf buffer) {
        return new EditorFilePayload(buffer.readUtf(64), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readUtf(EditorFileChunks.CHUNK_SIZE * 3));
    }
//?}

    /**
     * 将服务端分片交给客户端主线程，并保留当前连接校验。
     *
     * @param payload 服务端返回的响应分片
     */
    public static void handleOnClient(EditorFilePayload payload) {
        net.minecraft.client.Minecraft.getInstance().execute(() -> EditorFileClient.receive(payload));
    }

    /**
     * 将请求交给服务端主线程，权限以实际发包玩家为准。
     *
     * @param player 网络上下文认证的玩家
     * @param payload 客户端提交的请求分片
     */
    public static void handleOnServer(ServerPlayer player, EditorFilePayload payload) {
        player.server.execute(() -> EditorFileServer.receive(player, payload));
    }
}
