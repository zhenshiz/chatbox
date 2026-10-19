package com.zhenshiz.chatbox.editor;

import com.google.gson.*;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class EditorSocketServerTest {
    private EditorSocketServer server;
    private int port;
    private final java.util.concurrent.atomic.AtomicInteger disconnected = new java.util.concurrent.atomic.AtomicInteger();
    private final BlockingQueue<JsonObject> received = new LinkedBlockingQueue<>();

    @BeforeEach void start() throws Exception {
        server = new EditorSocketServer(request -> {
            if (request.get("type").getAsString().equals("fail")) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid dialogue"));
            JsonObject result = new JsonObject();
            result.addProperty("echo", request.get("type").getAsString());
            return CompletableFuture.completedFuture(result);
        }, disconnected::incrementAndGet);
        port = server.start(0);
    }

    @AfterEach void stop() { server.close(); }

    private WebSocket connect() {
        return HttpClient.newHttpClient().newWebSocketBuilder().header("Origin", "http://127.0.0.1:5173")
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/chatbox-editor"), new WebSocket.Listener() {
                    private final StringBuilder text = new StringBuilder();
                    @Override public void onOpen(WebSocket socket) { socket.request(1); }
                    @Override public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
                        text.append(data);
                        if (last) { received.add(JsonParser.parseString(text.toString()).getAsJsonObject()); text.setLength(0); }
                        socket.request(1);
                        return null;
                    }
                }).join();
    }

    private JsonObject send(WebSocket socket, String message) throws Exception {
        socket.sendText(message, true).join();
        JsonObject packet = received.poll(5, TimeUnit.SECONDS);
        assertNotNull(packet, "The server must respond within five seconds");
        return packet;
    }

    private void pair(WebSocket socket) throws Exception {
        assertTrue(send(socket, "{\"id\":\"hello\",\"type\":\"hello\",\"protocol\":1,\"token\":\"" + server.token() + "\"}").get("ok").getAsBoolean());
    }

    @Test void authenticatedRequestsAndErrorsPreserveRequestIds() throws Exception {
        WebSocket socket = connect();
        pair(socket);
        JsonObject result = send(socket, "{\"id\":\"42\",\"type\":\"snapshot\"}");
        assertEquals("42", result.get("id").getAsString());
        assertEquals("snapshot", result.getAsJsonObject("data").get("echo").getAsString());
        JsonObject error = send(socket, "{\"id\":\"43\",\"type\":\"fail\"}");
        assertFalse(error.get("ok").getAsBoolean());
        assertEquals("Invalid dialogue", error.get("error").getAsString());
        assertEquals("43", error.get("id").getAsString());
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
    }

    @Test void rejectsUnpairedDataAccess() throws Exception {
        assertFalse(send(connect(), "{\"id\":\"x\",\"type\":\"snapshot\"}").get("ok").getAsBoolean());
    }

    @Test void rejectsWrongTokenAndProtocol() throws Exception {
        assertFalse(send(connect(), "{\"id\":\"x\",\"type\":\"hello\",\"protocol\":1,\"token\":\"wrong\"}").get("ok").getAsBoolean());
        assertFalse(send(connect(), "{\"id\":\"y\",\"type\":\"hello\",\"protocol\":2,\"token\":\"" + server.token() + "\"}").get("ok").getAsBoolean());
    }

    @Test void rejectsASecondEditor() throws Exception {
        pair(connect());
        assertFalse(send(connect(), "{\"id\":\"x\",\"type\":\"hello\",\"protocol\":1,\"token\":\"" + server.token() + "\"}").get("ok").getAsBoolean());
    }

    @Test void broadcastsInvalidationAndReleasesPairingAfterDisconnect() throws Exception {
        WebSocket socket = connect();
        pair(socket);
        JsonObject event = new JsonObject();
        event.addProperty("event", "invalidate");
        server.broadcast(event);
        assertEquals("invalidate", received.poll(5, TimeUnit.SECONDS).get("event").getAsString());
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (disconnected.get() == 0 && System.nanoTime() < deadline) Thread.sleep(10);
        assertEquals(1, disconnected.get());
        pair(connect());
    }

    @Test void rejectsMissingOriginAndWrongEndpoint() {
        var builder = HttpClient.newHttpClient().newWebSocketBuilder();
        assertThrows(CompletionException.class, () -> builder.buildAsync(URI.create("ws://127.0.0.1:" + port + "/chatbox-editor"), new WebSocket.Listener() {}).join());
        assertThrows(CompletionException.class, () -> builder.header("Origin", "http://localhost:5173").buildAsync(URI.create("ws://127.0.0.1:" + port + "/other"), new WebSocket.Listener() {}).join());
    }
}
