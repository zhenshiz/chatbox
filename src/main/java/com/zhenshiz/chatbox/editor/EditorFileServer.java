package com.zhenshiz.chatbox.editor;

import com.google.gson.*;
import com.zhenshiz.chatbox.data.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** 校验四级权限，仅开放游戏已加载的 ChatBox 对话和主题资源。 */
public final class EditorFileServer {
    private static final String OVERRIDE = "chatbox_editor";
    private static final Map<MinecraftServer, String> sessions = new WeakHashMap<>();
    private static final Map<ServerPlayer, Map<String, EditorFileChunks>> incoming = new WeakHashMap<>();
    private static final Set<MinecraftServer> saving = Collections.newSetFromMap(new WeakHashMap<>());
    private EditorFileServer() { }

    /**
     * 接收每个请求分片前，重新校验实际服务端玩家的权限。
     * @param player 由游戏网络提供的实际发送者
     * @param packet 受大小限制的请求分片
     */
    public static void receive(ServerPlayer player, EditorFilePayload packet) {
        try {
            requireOperator(player);
            Map<String, EditorFileChunks> messages = incoming.computeIfAbsent(player, ignored -> new HashMap<>());
            messages.values().removeIf(EditorFileChunks::expired);
            EditorFileChunks chunks = messages.get(packet.requestId());
            if (chunks == null) {
                if (packet.index() != 0 || messages.size() >= 8) throw new IllegalArgumentException("编辑请求过多，请稍后重试");
                chunks = new EditorFileChunks(packet.total()); messages.put(packet.requestId(), chunks);
            }
            String text = chunks.append(packet.index(), packet.total(), packet.json());
            if (text == null) return;
            messages.remove(packet.requestId());
            JsonObject request = JsonParser.parseString(text).getAsJsonObject();
            execute(player, request).whenComplete((data, error) -> player.server.execute(() -> {
                // 数据重载期间玩家权限可能已被撤销。
                Throwable failure = error;
                if (failure == null && !player.createCommandSourceStack().hasPermission(4)) failure = new SecurityException("需要服务端 4 级权限");
                reply(player, packet.requestId(), data, failure);
            }));
        } catch (Exception error) {
            var messages = incoming.get(player);
            if (messages != null) messages.remove(packet.requestId());
            reply(player, packet.requestId(), null, error);
        }
    }

    private static void requireOperator(ServerPlayer player) {
        if (!player.createCommandSourceStack().hasPermission(4)) throw new SecurityException("需要服务端 4 级权限");
    }

    private static CompletableFuture<JsonObject> execute(ServerPlayer player, JsonObject request) throws IOException {
        requireOperator(player);
        MinecraftServer server = player.server;
        String op = request.get("op").getAsString();
        JsonObject data = new JsonObject();
        String session = sessions.computeIfAbsent(server, ignored -> UUID.randomUUID().toString());
        data.addProperty("serverSession", session); data.addProperty("permissionLevel", 4);
        if (op.equals("authorize")) { EditorDebugServer.touch(player); return CompletableFuture.completedFuture(data); }
        if (!session.equals(request.get("serverSession").getAsString())) throw new IllegalStateException("游戏连接已变化，请重新读取文件");
        if (op.startsWith("debug.")) return CompletableFuture.completedFuture(EditorDebugServer.execute(player, request));
        if (saving.contains(server)) throw new IllegalStateException("服务端正在重载数据，请稍后重试");
        if (op.equals("files.list")) {
            JsonArray files = new JsonArray();
            for (String kind : List.of("dialogue", "theme")) {
                for (ResourceLocation id : new TreeSet<>(map(kind).keySet())) files.add(describe(server, kind, id, resource(server, kind, id)));
            }
            data.add("files", files);
        } else {
            String kind = request.get("kind").getAsString();
            ResourceLocation id = ResourceLocation.parse(request.get("resourceId").getAsString());
            if (!map(kind).containsKey(id)) throw new IllegalArgumentException("该文件不在游戏已加载的 ChatBox 文件清单中");
            Resource resource = resource(server, kind, id);
            String content = read(resource);
            if (op.equals("files.read")) {
                data = describe(server, kind, id, resource);
                data.addProperty("content", content);
                data.addProperty("revision", EditorFileStore.revision(resource.sourcePackId(), content));
            } else if (op.equals("files.write")) return save(player, request, kind, id, resource, content);
            else throw new IllegalArgumentException("Unsupported editor file operation");
            data.addProperty("serverSession", session);
        }
        return CompletableFuture.completedFuture(data);
    }

    private static Map<ResourceLocation, String> map(String kind) {
        return switch (kind) { case "dialogue" -> ChatBoxDialoguesLoader.dialoguesMap; case "theme" -> ChatBoxThemeLoader.themeMap; default -> throw new IllegalArgumentException("Invalid ChatBox file kind"); };
    }

    private static Resource resource(MinecraftServer server, String kind, ResourceLocation id) {
        String folder = kind.equals("dialogue") ? "dialogues" : "theme";
        return server.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "chatbox/" + folder + "/" + id.getPath() + ".json")).orElseThrow(() -> new IllegalArgumentException("游戏资源文件已不存在，请刷新清单"));
    }

    private static String read(Resource resource) throws IOException {
        try (var stream = resource.open()) {
            byte[] bytes = stream.readNBytes(EditorFileStore.MAX_BYTES + 1);
            if (bytes.length > EditorFileStore.MAX_BYTES) throw new IOException("ChatBox file exceeds 1 MiB");
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private record Target(Path root, Path file, String mode) { }

    private static Target target(MinecraftServer server, String kind, ResourceLocation id, Resource resource) throws IOException {
        Path datapacks = server.getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
        String source = resource.sourcePackId();
        if (resource.source() instanceof PathPackResources && source.matches("file/[^/\\\\]+") && !source.substring(5).equals(".") && !source.substring(5).equals("..")) {
            Path pack = EditorFileStore.contained(datapacks, source.substring(5));
            Path file = EditorFileStore.resolve(pack, kind, id.toString());
            if (Files.isRegularFile(file) && Files.isWritable(file) && Files.isWritable(file.getParent())) return new Target(pack, file, "original");
        }
        Path pack = EditorFileStore.contained(datapacks, OVERRIDE);
        return new Target(pack, EditorFileStore.resolve(pack, kind, id.toString()), "override");
    }

    private static JsonObject describe(MinecraftServer server, String kind, ResourceLocation id, Resource resource) throws IOException {
        Target target = target(server, kind, id, resource);
        JsonObject data = new JsonObject();
        data.addProperty("kind", kind); data.addProperty("resourceId", id.toString());
        data.addProperty("source", resource.sourcePackId()); data.addProperty("saveMode", target.mode);
        data.addProperty("savePath", "datapacks/" + target.root.getFileName() + "/" + target.root.relativize(target.file).toString().replace('\\', '/'));
        return data;
    }

    private static CompletableFuture<JsonObject> save(ServerPlayer player, JsonObject request, String kind, ResourceLocation id, Resource source, String current) throws IOException {
        MinecraftServer server = player.server;
        if (!EditorFileStore.revision(source.sourcePackId(), current).equals(request.get("revision").getAsString())) throw new IllegalStateException("文件已被修改，请重新读取后再保存");
        String content = request.get("content").getAsString();
        JsonObject parsed = EditorFileStore.parse(content);
        if (kind.equals("dialogue")) {
            if (!parsed.has("dialogues") || !parsed.get("dialogues").isJsonObject()) throw new IllegalArgumentException("对话文件必须包含 dialogues 对象");
            ChatBoxDialogues model = ChatBoxDialoguesLoader.GSON.fromJson(parsed, ChatBoxDialogues.class);
            if (model.dialogues.values().stream().anyMatch(frames -> frames == null || frames.stream().anyMatch(frame -> frame == null || frame.dialogBox == null))) throw new IllegalArgumentException("对话帧格式错误");
        } else {
            ChatBoxTheme model = ChatBoxDialoguesLoader.GSON.fromJson(parsed, ChatBoxTheme.class);
            if (model.dialogBox == null || model.option == null || model.keyPrompt == null || model.portrait == null
                    || model.functionalButton == null || model.customAnimation == null
                    || model.portrait.containsValue(null) || model.functionalButton.contains(null))
                throw new IllegalArgumentException("主题核心组件不能为 null");
        }
        Target target = target(server, kind, id, source);
        // 即使尚未执行 /reload，目录数据包中的内容也必须与当前生效的资源一致。
        if (target.mode.equals("original") && !Files.readString(target.file).equals(current)) throw new IllegalStateException("文件已被修改，请重新读取后再保存");
        boolean existed = Files.exists(target.file);
        byte[] previous = existed ? Files.readAllBytes(target.file) : null;
        saving.add(server);
        try {
            if (target.mode.equals("override")) {
                Files.createDirectories(target.root);
                Path metadata = EditorFileStore.contained(target.root, "pack.mcmeta");
                if (!Files.exists(metadata)) Files.writeString(metadata, "{\"pack\":{\"pack_format\":48,\"description\":\"ChatBox website editor overrides\"}}\n");
            }
            EditorFileStore.write(target.root, target.file, content);
            server.getPackRepository().reload();
            List<String> packs = new ArrayList<>(server.getPackRepository().getSelectedIds());
            if (target.mode.equals("override")) { packs.remove("file/" + OVERRIDE); packs.add("file/" + OVERRIDE); }
            return server.reloadResources(packs).handleAsync((ignored, error) -> {
                saving.remove(server);
                if (error != null) {
                    try {
                        if (existed) Files.write(target.file, previous); else Files.deleteIfExists(target.file);
                    } catch (IOException rollback) { error.addSuppressed(rollback); }
                    throw new java.util.concurrent.CompletionException(new IllegalStateException("数据包重载失败，已尝试恢复文件", error));
                }
                try {
                    Resource loaded = resource(server, kind, id);
                    JsonObject data = describe(server, kind, id, loaded);
                    data.addProperty("revision", EditorFileStore.revision(loaded.sourcePackId(), read(loaded)));
                    data.addProperty("serverSession", sessions.get(server));
                    return data;
                } catch (IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
            }, server);
        } catch (Exception error) { saving.remove(server); throw error; }
    }

    private static void reply(ServerPlayer player, String id, JsonObject data, Throwable error) {
        JsonObject response = new JsonObject(); response.addProperty("ok", error == null);
        if (error == null) response.add("data", data);
        else {
            while (error.getCause() != null) error = error.getCause();
            response.addProperty("error", error.getMessage() == null ? "服务端文件操作失败" : error.getMessage());
        }
        List<String> chunks = EditorFileChunks.split(response.toString());
        for (int i = 0; i < chunks.size(); i++) player.connection.send(new EditorFilePayload(id, i, chunks.size(), chunks.get(i)));
    }
}
