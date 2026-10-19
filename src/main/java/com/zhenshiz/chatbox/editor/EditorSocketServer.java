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
 * Serves one authenticated editor on the IPv4 loopback interface.
 *
 * <p>JSON responses contain the request's string {@code id}, an {@code ok} boolean,
 * and either {@code data} or {@code error}. Authentication requires protocol version
 * {@value #PROTOCOL} and the session token; frame payloads are bounded to 16 MiB.
 */
public final class EditorSocketServer implements AutoCloseable {
    /** The protocol version required in the initial editor handshake. */
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
     * Creates an inactive bridge.
     *
     * @param dispatcher the asynchronous handler for authenticated requests
     * @param disconnected the callback invoked when the paired editor disconnects
     */
    public EditorSocketServer(Function<JsonObject, CompletableFuture<JsonObject>> dispatcher, Runnable disconnected) {
        this.dispatcher = dispatcher;
        this.disconnected = disconnected;
    }

    /**
     * Binds the local listener.
     *
     * @param port the port in the range 0 through 65535; zero selects a free port
     * @return the actual listening port
     * @throws Exception if binding fails or exceeds five seconds
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
     * Returns the secret valid for this listener's lifetime.
     *
     * @return the session authentication token
     */
    public String token() { return token; }

    /**
     * Sends a server event to the paired editor.
     *
     * @param event the JSON event without a request identifier
     */
    public void broadcast(JsonObject event) {
        WebSocket socket = editor;
        if (socket != null) write(socket, GSON.toJson(event));
    }

    /**
     * Checks whether the paired editor can accept another preview frame.
     *
     * @return {@code true} if the socket is open and has no buffered writes
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
        catch (WebsocketNotConnectedException ignored) { /* The peer closed between the check and send. */ }
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
