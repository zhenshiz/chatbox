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
 * Carries one bounded chunk of an editor file request or response over the player's game connection.
 * @param requestId the client-generated request UUID
 * @param index the zero-based chunk index
 * @param total the total chunk count
 * @param json the JSON fragment, at most 8,000 UTF-16 code units
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
     * Routes messages by the authenticated network direction on the corresponding main thread.
     * @param payload the received chunk
     * @param context the player's actual network context
     */
    public static void handle(EditorFilePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) EditorFileServer.receive(player, payload);
        else EditorFileClient.receive(payload);
    }

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
