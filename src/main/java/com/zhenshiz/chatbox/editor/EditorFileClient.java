package com.zhenshiz.chatbox.editor;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
import java.util.concurrent.*;

/** Correlates server-authorized file operations with the current Minecraft client connection. */
public final class EditorFileClient {
    private record Pending(Object connection, CompletableFuture<JsonObject> result, EditorFileChunks chunks) { }
    private static final Map<String, Pending> pending = new HashMap<>();
    private EditorFileClient() { }

    /**
     * Sends an operation to the current server; all access is checked by that server.
     * @param request the JSON operation and optional resource ID, revision, and content
     * @return a future completed on the client thread with the server response
     */
    public static CompletableFuture<JsonObject> request(JsonObject request) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null)
            return CompletableFuture.failedFuture(new IllegalStateException("请先进入游戏世界"));
        if (!mc.getConnection().hasChannel(EditorFilePayload.TYPE))
            return CompletableFuture.failedFuture(new IllegalStateException("服务端未安装支持网页编辑的新版本 ChatBox"));
        if (pending.size() >= 8) return CompletableFuture.failedFuture(new IllegalStateException("编辑请求过多，请稍后重试"));
        String id = UUID.randomUUID().toString();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        pending.put(id, new Pending(mc.getConnection(), result, null));
        try {
            var chunks = EditorFileChunks.split(request.toString());
            for (int i = 0; i < chunks.size(); i++) PacketDistributor.sendToServer(new EditorFilePayload(id, i, chunks.size(), chunks.get(i)));
        } catch (Exception error) { pending.remove(id); result.completeExceptionally(error); }
        CompletableFuture.delayedExecutor(90, TimeUnit.SECONDS).execute(() -> mc.execute(() -> {
            Pending old = pending.remove(id);
            if (old != null) old.result.completeExceptionally(new IllegalStateException("服务端编辑请求超时，请确认服务端已安装新版 ChatBox"));
        }));
        return result;
    }

    /**
     * Assembles a response only while the originating game connection is still current.
     * @param packet the response fragment
     */
    public static void receive(EditorFilePayload packet) {
        Pending item = pending.get(packet.requestId());
        if (item == null) return;
        try {
            if (item.connection != Minecraft.getInstance().getConnection()) throw new IllegalStateException("游戏连接已变化，请重新连接编辑器");
            if (item.chunks == null) {
                item = new Pending(item.connection, item.result, new EditorFileChunks(packet.total()));
                pending.put(packet.requestId(), item);
            }
            String text = item.chunks.append(packet.index(), packet.total(), packet.json());
            if (text == null) return;
            pending.remove(packet.requestId());
            JsonObject response = JsonParser.parseString(text).getAsJsonObject();
            if (response.get("ok").getAsBoolean()) item.result.complete(response.getAsJsonObject("data"));
            else item.result.completeExceptionally(new IllegalStateException(response.get("error").getAsString()));
        } catch (Exception error) { pending.remove(packet.requestId()); item.result.completeExceptionally(error); }
    }

    /** Cancels in-flight operations when the game connection closes. */
    public static void disconnected() {
        var requests = new ArrayList<>(pending.values()); pending.clear();
        requests.forEach(item -> item.result.completeExceptionally(new IllegalStateException("游戏连接已断开")));
    }
}
