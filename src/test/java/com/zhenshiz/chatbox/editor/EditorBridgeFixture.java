package com.zhenshiz.chatbox.editor;

import com.google.gson.*;
import java.util.concurrent.*;

/** Runs the production WebSocket transport with explicit fixture data for browser integration tests. */
public final class EditorBridgeFixture {
    private EditorBridgeFixture() { }

    /**
     * Starts the fixture until the process is terminated.
     *
     * @param args unused command-line arguments
     * @throws Exception if the fixture cannot bind or is interrupted
     */
    public static void main(String[] args) throws Exception {
        var server = new EditorSocketServer(request -> {
            String type = request.get("type").getAsString();
            JsonObject response = switch (type) {
                case "snapshot" -> JsonParser.parseString("""
                    {"protocol":1,"minecraft":"1.21.1 (fixture)","chatbox":"bridge fixture","language":"zh_cn","inWorld":true,
                    "translations":{"fixture.dialogue.welcome":"来自游戏数据源的测试译文","item.fixture.crystal":"测试水晶"},
                    "registries":{"ITEM":[{"label":"测试水晶","value":"fixture:crystal","itemId":"fixture:crystal"}],
                    "ATTRIBUTE":[{"label":"测试属性","value":"fixture:power"}],"ENCHANTMENT":[{"label":"测试附魔","value":"fixture:glow"}],
                    "TEXTURE":[{"label":"fixture:textures/gui/dialogue.png","value":"fixture:textures/gui/dialogue.png","source":"Fixture mod resources"}]},
                    "themes":[{"label":"fixture:default","value":"fixture:default"}]}
                    """).getAsJsonObject();
                case "files.list" -> JsonParser.parseString("{\"serverSession\":\"fixture\",\"files\":[]}").getAsJsonObject();
                case "image" -> {
                    JsonObject image = new JsonObject();
                    image.addProperty("url", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
                    yield image;
                }
                case "preview" -> {
                    JsonObject preview = new JsonObject();
                    preview.addProperty("group", request.get("group").getAsString());
                    preview.addProperty("index", request.get("index").getAsInt());
                    yield preview;
                }
                default -> new JsonObject();
            };
            return CompletableFuture.completedFuture(response);
        }, () -> {});
        int port = server.start(0);
        System.out.println("FIXTURE_ADDRESS=ws://127.0.0.1:" + port + "/chatbox-editor");
        System.out.println("FIXTURE_TOKEN=" + server.token());
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        new CountDownLatch(1).await();
    }
}
