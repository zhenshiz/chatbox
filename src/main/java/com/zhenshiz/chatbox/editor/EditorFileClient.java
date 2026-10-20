package com.zhenshiz.chatbox.editor;

import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.concurrent.*;

/** 将服务端鉴权后的文件操作关联到当前 Minecraft 客户端连接。 */
public final class EditorFileClient {
    private record Pending(Object connection, CompletableFuture<JsonObject> result, EditorFileChunks chunks) { }
    private static final Map<String, Pending> pending = new HashMap<>();
    private EditorFileClient() { }

    /**
     * 向当前服务端发送操作，所有访问均由该服务端校验。
     * @param request 包含操作及可选资源 ID、版本和内容的 JSON 请求
     * @return 在客户端线程上接收服务端响应后完成的异步结果
     */
    public static CompletableFuture<JsonObject> request(JsonObject request) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null)
            return CompletableFuture.failedFuture(new IllegalStateException("请先进入游戏世界"));
        if (!EditorNetwork.canSend())
            return CompletableFuture.failedFuture(new IllegalStateException("服务端未安装支持网页编辑的新版本 ChatBox"));
        if (pending.size() >= 8) return CompletableFuture.failedFuture(new IllegalStateException("编辑请求过多，请稍后重试"));
        String id = UUID.randomUUID().toString();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        pending.put(id, new Pending(mc.getConnection(), result, null));
        try {
            var chunks = EditorFileChunks.split(request.toString());
            for (int i = 0; i < chunks.size(); i++) EditorNetwork.sendToServer(new EditorFilePayload(id, i, chunks.size(), chunks.get(i)));
        } catch (Exception error) { pending.remove(id); result.completeExceptionally(error); }
        CompletableFuture.delayedExecutor(90, TimeUnit.SECONDS).execute(() -> mc.execute(() -> {
            Pending old = pending.remove(id);
            if (old != null) old.result.completeExceptionally(new IllegalStateException("服务端编辑请求超时，请确认服务端已安装新版 ChatBox"));
        }));
        return result;
    }

    /**
     * 仅当原游戏连接仍然有效时组装响应。
     * @param packet 响应分片
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

    /** 游戏连接关闭时取消尚未完成的操作。 */
    public static void disconnected() {
        var requests = new ArrayList<>(pending.values()); pending.clear();
        requests.forEach(item -> item.result.completeExceptionally(new IllegalStateException("游戏连接已断开")));
    }
}
