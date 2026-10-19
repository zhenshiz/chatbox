package com.zhenshiz.chatbox.editor;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 通过玩家游戏连接传输编辑器文件请求或响应的一个受限分片。
 * @param requestId 客户端生成的请求 UUID
 * @param index 从 0 开始的分片索引
 * @param total 分片总数
 * @param json JSON 分片，最多 8000 个 UTF-16 代码单元
 */
public record EditorFilePayload(String requestId, int index, int total, String json) implements CustomPacketPayload {
    public static final Type<EditorFilePayload> TYPE = new Type<>(ChatBox.id("editor_files"));
    public static final StreamCodec<FriendlyByteBuf, EditorFilePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), EditorFilePayload::requestId,
            ByteBufCodecs.VAR_INT, EditorFilePayload::index,
            ByteBufCodecs.VAR_INT, EditorFilePayload::total,
            ByteBufCodecs.stringUtf8(EditorFileChunks.CHUNK_SIZE * 3), EditorFilePayload::json,
            EditorFilePayload::new);

    /**
     * 根据已认证的网络方向，将消息分派到对应的主线程。
     * @param payload 收到的分片
     * @param context 玩家实际的网络上下文
     */
    public static void handle(EditorFilePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) EditorFileServer.receive(player, payload);
        else EditorFileClient.receive(payload);
    }

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
