package com.zhenshiz.chatbox.editor;

import com.google.gson.*;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.*;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 在 IPv4 回环地址上为一个已认证的编辑器提供服务。
 *
 * <p>JSON 响应包含请求字符串 {@code id}、布尔值 {@code ok}，
 * 以及 {@code data} 或 {@code error}。认证要求协议版本为
 * {@value #PROTOCOL} 并提供会话令牌；帧载荷最大为 16 MiB。
 */
public final class EditorSocketServer implements AutoCloseable {
    /** 编辑器首次握手要求的协议版本。 */
    public static final int PROTOCOL = 1;
    private static final Gson GSON = new Gson();
    private final String token = UUID.randomUUID().toString().replace("-", "");
    private final Function<JsonObject, CompletableFuture<JsonObject>> dispatcher;
    private final Runnable disconnected;
    private final CompletableFuture<Integer> started = new CompletableFuture<>();
    private final Map<WebSocket, AtomicInteger> pending = new ConcurrentHashMap<>();
    private volatile WebSocket editor;
    private WebSocketServer listener;

    /**
     * 创建尚未启动监听的桥接服务。
     *
     * @param dispatcher 处理已认证请求的异步回调
     * @param disconnected 配对编辑器断开连接时的回调
     */
    public EditorSocketServer(Function<JsonObject, CompletableFuture<JsonObject>> dispatcher, Runnable disconnected) {
        this.dispatcher = dispatcher;
        this.disconnected = disconnected;
    }

    /**
     * 启动本机监听。
     *
     * @param port 0 到 65535 之间的端口；0 表示自动选择空闲端口
     * @return 实际监听的端口
     * @throws Exception 绑定失败或超过五秒时抛出
     */
    public int start(int port) throws Exception {
        listener = new WebSocketServer(new InetSocketAddress("127.0.0.1", port), 1,
                List.of(new Draft_6455(List.of(), 16 * 1024 * 1024))) {
            @Override public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket socket, org.java_websocket.drafts.Draft draft, ClientHandshake request) throws InvalidDataException {
                try {
                    URI origin = URI.create(request.getFieldValue("Origin"));
                    if (!("http".equals(origin.getScheme()) || "https".equals(origin.getScheme())) || origin.getHost() == null
                            || !request.getFieldValue("Host").matches("(127\\.0\\.0\\.1|localhost):[0-9]+")
                            || !request.getResourceDescriptor().equals("/chatbox-editor")) throw new IllegalArgumentException();
                } catch (IllegalArgumentException error) { throw new InvalidDataException(1008, "Invalid editor origin or endpoint"); }
                return super.onWebsocketHandshakeReceivedAsServer(socket, draft, request);
            }
            @Override public void onOpen(WebSocket socket, ClientHandshake handshake) {
                pending.put(socket, new AtomicInteger());
                CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
                    if (socket != editor && socket.isOpen()) socket.close(1008, "Authentication timed out");
                });
            }
            @Override public void onMessage(WebSocket socket, String text) { receive(socket, text); }
            @Override public void onClose(WebSocket socket, int code, String reason, boolean remote) {
                pending.remove(socket);
                if (editor == socket) { editor = null; disconnected.run(); }
            }
            @Override public void onError(WebSocket socket, Exception error) {
                if (!started.isDone()) started.completeExceptionally(error);
                if (socket != null) socket.close(1011, "Connection error");
            }
            @Override public void onStart() { started.complete(getPort()); }
        };
        listener.setDaemon(true);
        listener.setConnectionLostTimeout(30);
        listener.start();
        return started.get(5, TimeUnit.SECONDS);
    }

    /**
     * 获取本次监听有效的认证令牌。
     *
     * @return 会话认证令牌
     */
    public String token() { return token; }

    /**
     * 向配对编辑器推送服务端事件。
     *
     * @param event 不带请求标识符的 JSON 事件
     */
    public void broadcast(JsonObject event) {
        WebSocket socket = editor;
        if (socket != null) write(socket, GSON.toJson(event));
    }

    /**
     * 判断配对编辑器是否可以接收下一帧预览。
     *
     * @return 套接字已连接且没有待发送数据时返回 {@code true}
     */
    public boolean canSendFrame() {
        WebSocket socket = editor;
        return socket != null && socket.isOpen() && !socket.hasBufferedData();
    }

    @Override public void close() {
        if (listener != null) {
            try { listener.stop(1000); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        }
    }

    private void receive(WebSocket socket, String text) {
        String id = "";
        try {
            JsonObject request = JsonParser.parseString(text).getAsJsonObject();
            id = request.get("id").getAsString();
            if (id.length() > 80) throw new IllegalArgumentException("Invalid request id");
            if (socket != editor) {
                boolean valid = "hello".equals(request.get("type").getAsString())
                        && request.get("protocol").getAsInt() == PROTOCOL
                        && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), request.get("token").getAsString().getBytes(StandardCharsets.UTF_8));
                synchronized (this) {
                    if (!valid || editor != null) {
                        send(socket, id, null, "Pairing failed, protocol mismatch, or another editor is connected");
                        socket.close(1008, "Pairing failed");
                        return;
                    }
                    editor = socket;
                }
                send(socket, id, new JsonObject(), null);
                return;
            }
            AtomicInteger count = pending.get(socket);
            if (count == null) return;
            if (count.incrementAndGet() > 32) { count.decrementAndGet(); throw new IllegalArgumentException("Too many pending requests"); }
            String requestId = id;
            try {
                dispatcher.apply(request).whenComplete((data, error) -> {
                    count.decrementAndGet();
                    send(socket, requestId, data, error == null ? null : rootMessage(error));
                });
            } catch (Exception error) { count.decrementAndGet(); throw error; }
        } catch (Exception error) {
            send(socket, id, null, rootMessage(error));
            if (socket != editor) socket.close(1008, "Pairing failed");
        }
    }

    private static String rootMessage(Throwable error) {
        while (error.getCause() != null) error = error.getCause();
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static void write(WebSocket socket, String text) {
        try { if (socket.isOpen()) socket.send(text); }
        catch (WebsocketNotConnectedException ignored) { /* 检查后、发送前对端已关闭连接。 */ }
    }

    private static void send(WebSocket socket, String id, JsonObject data, String error) {
        if (!socket.isOpen()) return;
        JsonObject response = new JsonObject();
        response.addProperty("id", id);
        response.addProperty("ok", error == null);
        if (error == null) response.add("data", data);
        else response.addProperty("error", error);
        write(socket, GSON.toJson(response));
    }
}
