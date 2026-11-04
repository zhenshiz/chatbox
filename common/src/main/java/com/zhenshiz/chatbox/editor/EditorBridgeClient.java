package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.LivingEntity;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static net.minecraft.commands.Commands.literal;

/**
 * 向已配对的网站开放当前客户端资源与临时对话预览。
 *
 * <p>游戏访问在客户端线程上执行，桥接服务通过
 * {@code /chatbox editor start} 显式启动，同一时间只允许一个编辑器认证连接。
 */
public final class EditorBridgeClient {
    public static final Minecraft mc = Minecraft.getInstance();
    private static volatile EditorSocketServer server;
    private static int port;
    private static Language lastLanguage;
    private static Object lastLevel;
    private static int ticks;
    private static String serverSession;
    private static boolean checkingPermission;
    private static int startAttempt;
    private static final Map<String, JsonObject> imageCache = new LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, JsonObject> entry) { return size() > 256; }
    };

    private EditorBridgeClient() { }

    /**
     * 注册本机桥接服务的启动与关闭命令。
     */
    public static void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("chatbox")
                .then(literal("editor")
                .then(literal("start").executes(context -> {
                    int attempt = ++startAttempt;
                    var connection = mc.getConnection();
                    authorize().whenComplete((permission, failure) -> {
                        if (attempt != startAttempt || connection != mc.getConnection()) return;
                        try {
                            if (failure != null) throw new IllegalStateException(failure.getMessage(), failure);
                            serverSession = permission.get("serverSession").getAsString();
                            if (server == null) {
                                server = new EditorSocketServer(EditorBridgeClient::dispatch, () -> mc.execute(EditorBridgeClient::disconnected));
                                port = server.start(0);
                                lastLanguage = Language.getInstance();
                                lastLevel = mc.level;
                            }
                            String address = "ws://127.0.0.1:" + port + "/chatbox-editor";
                            String token = server.token();
                            context.getSource().sendSuccess(() -> Component.translatable("chatbox.editor.connection", copyable(address)), false);
                            context.getSource().sendSuccess(() -> Component.translatable("chatbox.editor.token", copyable(token)), false);
                            context.getSource().sendSuccess(() -> Component.translatable("chatbox.editor.instructions"), false);
                        } catch (Exception error) {
                            closeBridge();
                            context.getSource().sendFailure(Component.translatable("chatbox.editor.error", error.getMessage()));
                        }
                    });
                    return 1;
                }))
                .then(literal("stop").executes(context -> {
                    closeBridge();
                    context.getSource().sendSuccess(() -> Component.translatable("chatbox.editor.stopped"), false);
                    return 1;
                }))));
    }

    private static Component copyable(String value) {
        return Component.literal(value).withStyle(style -> style
                .withColor(net.minecraft.ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(value))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chatbox.editor.copy"))));
    }

    /**
     * 客户端语言、资源或世界变化后，使网页缓存失效。
     */
    public static void tick() {
        if (server == null || ++ticks % 20 != 0) return;
        if (mc.level == null || lastLevel != mc.level) { closeBridge(); return; }
        if (!checkingPermission) {
            checkingPermission = true;
            EditorSocketServer expected = server;
            authorize().whenComplete((permission, error) -> {
                if (server != expected) return;
                checkingPermission = false;
                if (error != null || !permission.get("serverSession").getAsString().equals(serverSession)) closeBridge();
            });
        }
        if (lastLanguage != Language.getInstance()) {
            if (lastLevel != mc.level) EditorPreview.stop();
            lastLanguage = Language.getInstance();
            lastLevel = mc.level;
            invalidate();
        }
    }

    private static CompletableFuture<JsonObject> authorize() {
        JsonObject request = new JsonObject();
        request.addProperty("op", "authorize");
        return EditorFileClient.request(request);
    }

    private static void closeBridge() {
        ++startAttempt;
        if (server != null) server.close();
        server = null; serverSession = null; checkingPermission = false;
        disconnected();
    }

    /**
     * 游戏连接关闭时结束编辑器会话，并取消待完成的服务端操作。
     */
    public static void logout() {
        closeBridge();
        EditorFileClient.disconnected();
    }

    private static void invalidate() {
        imageCache.clear();
        if (server != null) {
            JsonObject event = new JsonObject();
            event.addProperty("event", "invalidate");
            server.broadcast(event);
        }
    }

    private static void disconnected() {
        EditorDebug.stop();
        EditorPreview.stop();
        try { EditorPreviewPack.commit(Set.of(), new JsonObject()); }
        catch (Exception error) { ChatBox.LOGGER.warn("Could not clear editor preview resources", error); }
        imageCache.clear();
    }

    private static CompletableFuture<JsonObject> dispatch(JsonObject request) {
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        mc.execute(() -> {
            String type = request.get("type").getAsString();
            if (type.startsWith("files.")) {
                JsonObject operation = request.deepCopy();
                operation.addProperty("op", type);
                // 网页会话必须属于读取该文件时所在的世界。
                if (!operation.has("serverSession")) operation.addProperty("serverSession", serverSession);
                EditorFileClient.request(operation).whenComplete((data, error) -> {
                    if (error != null) result.completeExceptionally(error); else result.complete(data);
                });
                return;
            }
            if (type.equals("debug.start") || type.equals("debug.update") || type.equals("debug.stop")) {
                // 草稿请求仍需通过服务端文件网关鉴权。提前登记启动请求，
                // 确保等待服务端响应时收到的停止请求也能取消启动。
                try {
                    request.addProperty("serverSession", serverSession);
                    if (type.equals("debug.stop")) {
                        EditorDebug.stop(request);
                        result.complete(new JsonObject());
                    } else {
                        var debug = type.equals("debug.start") ? EditorDebug.start(request) : EditorDebug.update(request);
                        debug.whenComplete((data, error) -> {
                            if (error != null) result.completeExceptionally(error); else result.complete(data);
                        });
                    }
                } catch (Exception error) { result.completeExceptionally(error); }
                return;
            }
            EditorSocketServer expected = server;
            authorize().whenComplete((permission, failure) -> {
            if (server != expected || expected == null) { result.completeExceptionally(new IllegalStateException("游戏连接已变化，请重新连接编辑器")); return; }
            if (failure != null || !permission.get("serverSession").getAsString().equals(serverSession)) {
                result.completeExceptionally(new SecurityException("需要服务端 4 级权限"));
                closeBridge(); return;
            }
            try {
                if (type.equals("assets.commit")) {
                    Set<String> ids = new HashSet<>();
                    request.getAsJsonArray("images").forEach(id -> ids.add(id.getAsString()));
                    EditorPreviewPack.commit(ids, request.getAsJsonObject("translations")).whenComplete((ignored, error) -> {
                        if (error != null) result.completeExceptionally(error);
                        else { imageCache.clear(); result.complete(new JsonObject()); }
                    });
                    return;
                }
                JsonObject data = switch (type) {
                    case "ping" -> new JsonObject();
                    case "snapshot" -> snapshot();
                    case "image" -> image(request);
                    case "asset.upload" -> {
                        EditorPreviewPack.upload(request.get("resourceId").getAsString(), request.get("base64").getAsString());
                        yield new JsonObject();
                    }
                    case "preview" -> EditorPreview.show(request);
                    case "preview.move" -> EditorPreview.move(request);
                    case "preview.animation" -> EditorPreview.animation(request);
                    case "preview.stop" -> { EditorPreview.stop(); yield new JsonObject(); }
                    default -> throw new IllegalArgumentException("Unsupported editor request: " + type);
                };
                result.complete(data);
            } catch (Exception error) { result.completeExceptionally(error); }
            });
        });
        return result;
    }

    static boolean canSendFrame() { return server != null && server.canSendFrame(); }

    static String serverSession() { return serverSession; }

    static void sendEvent(JsonObject event) { if (server != null) server.broadcast(event); }

    static void sendFrame(JsonObject frame) {
        EditorSocketServer current = server;
        if (current != null && current.canSendFrame()) current.broadcast(frame);
    }

    /**
     * 只采集属于临时编辑器界面的完整渲染帧。
     */
    public static void renderFrame() { EditorPreview.captureFrame(); }

    private static JsonObject snapshot() {
        JsonObject result = new JsonObject();
        result.addProperty("protocol", EditorSocketServer.PROTOCOL);
        result.addProperty("previewStream", true);
        result.addProperty("previewMove", true);
        result.addProperty("previewTransform", true);
        result.addProperty("previewAnimation", true);
        result.addProperty("dialogueDebug", true);
        result.addProperty("minecraft", net.minecraft.SharedConstants.getCurrentVersion().name());
        result.addProperty("chatbox", ChatBox.PLATFORM.getModVersion(ChatBox.MOD_ID));
        result.addProperty("language", mc.getLanguageManager().getSelected());
        result.addProperty("inWorld", mc.level != null);
        Map<String, String> translations = Language.getInstance() instanceof ClientLanguage language ? language.storage : Map.of();
        result.add("translations", ChatBoxUtil.GSON.toJsonTree(translations));
        JsonObject registries = new JsonObject();
        registries.add("ITEM", entries(BuiltInRegistries.ITEM, item -> Component.translatable(item.getDescriptionId()).getString(), true));
        registries.add("BLOCK", entries(BuiltInRegistries.BLOCK, block -> block.getName().getString(), true));
        registries.add("EFFECT", entries(BuiltInRegistries.MOB_EFFECT, effect -> effect.getDisplayName().getString(), false));
        registries.add("ATTRIBUTE", entries(BuiltInRegistries.ATTRIBUTE, attribute -> Component.translatable(attribute.getDescriptionId()).getString(), false));
        registries.add("ENTITY", entries(BuiltInRegistries.ENTITY_TYPE, entity -> entity.getDescription().getString(), false));
        JsonArray living = new JsonArray();
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            if (LivingEntity.class.isAssignableFrom(type.getBaseClass())) living.add(option(type.getDescription().getString(), BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        }
        registries.add("LIVING_ENTITY", living);
        registries.add("SOUND", entries(BuiltInRegistries.SOUND_EVENT, sound -> sound.location().toString(), false));
        registries.add("PARTICLE", entries(BuiltInRegistries.PARTICLE_TYPE, particle -> BuiltInRegistries.PARTICLE_TYPE.getKey(particle).toString(), false));
        registries.add("FLUID", entries(BuiltInRegistries.FLUID, fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString(), false));
        addTags(registries, "ITEM_TAG", BuiltInRegistries.ITEM);
        addTags(registries, "BLOCK_TAG", BuiltInRegistries.BLOCK);
        addTags(registries, "ENTITY_TAG", BuiltInRegistries.ENTITY_TYPE);
        if (mc.level != null) {
            var enchantments = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            registries.add("ENCHANTMENT", entries(enchantments, enchantment -> enchantment.description().getString(), false));
            addTags(registries, "ENCHANTMENT_TAG", enchantments);
            var biomes = mc.level.registryAccess().lookupOrThrow(Registries.BIOME);
            registries.add("BIOME", entries(biomes, biome -> biomes.getKey(biome).toString(), false));
            addTags(registries, "BIOME_TAG", biomes);
        }
        registries.add("TEXTURE", EditorTextureCatalog.collect(mc.getResourceManager()));
        result.add("registries", registries);
        JsonArray themes = new JsonArray();
        ChatBoxUtil.themeMap.keySet().stream().sorted().forEach(id -> themes.add(option(id.toString(), id.toString())));
        result.add("themes", themes);
        return result;
    }

    static void previewClosed(String revision) {
        if (server == null) return;
        JsonObject event = new JsonObject();
        event.addProperty("event", "preview.closed");
        event.addProperty("revision", revision);
        server.broadcast(event);
    }

    static void previewError(RuntimeException error) {
        if (server != null) {
            JsonObject event = new JsonObject();
            event.addProperty("event", "preview.error");
            event.addProperty("error", error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
            server.broadcast(event);
        }
        ChatBox.LOGGER.warn("Editor preview failed", error);
        EditorPreview.stop();
    }

    private static <T> JsonArray entries(Registry<T> registry, Function<T, String> label, boolean itemIcon) {
        JsonArray array = new JsonArray();
        for (T value : registry) {
            String id = registry.getKey(value).toString();
            JsonObject option = option(label.apply(value), id);
            if (itemIcon) {
                String itemId = value instanceof net.minecraft.world.level.block.Block block ? BuiltInRegistries.ITEM.getKey(block.asItem()).toString() : id;
                option.addProperty("itemId", itemId);
            }
            if (value instanceof net.minecraft.world.effect.MobEffect) option.addProperty("textureId", id.replace(":", ":textures/mob_effect/") + ".png");
            array.add(option);
        }
        return array;
    }

    private static void addTags(JsonObject registries, String name, Registry<?> registry) {
        JsonArray tags = new JsonArray();
        registry.getTags().sorted(Comparator.comparing(tag -> tag.key().location().toString()))
                .forEach(tag -> tags.add(option("#" + tag.key().location(), "#" + tag.key().location())));
        registries.add(name, tags);
    }

    private static JsonObject option(String label, String value) {
        JsonObject option = new JsonObject();
        option.addProperty("label", label);
        option.addProperty("value", value);
        return option;
    }

    private static JsonObject image(JsonObject request) throws Exception {
        String kind = request.get("kind").getAsString();
        var id = ChatBox.parseId(request.get("resourceId").getAsString());
        int customData = request.has("customModelData") ? request.get("customModelData").getAsInt() : 0;
        String key = kind + ":" + id + ":" + customData;
        if (imageCache.containsKey(key)) return imageCache.get(key);
        JsonObject result;
        if (kind.equals("item")) result = ImageExport.renderItem(id);
        else if (kind.equals("texture")) {
            if (!id.getPath().startsWith("textures/") || !id.getPath().matches(".*\\.(png|jpg|jpeg)$")) throw new IllegalArgumentException("Invalid texture resource");
            var resource = mc.getResourceManager().getResource(id).orElseThrow(() -> new IllegalArgumentException("Texture not found: " + id));
            try (InputStream input = resource.open()) {
                byte[] bytes = input.readNBytes(8 * 1024 * 1024 + 1);
                if (bytes.length > 8 * 1024 * 1024) throw new IllegalArgumentException("Texture exceeds 8 MiB");
                result = new JsonObject();
                result.addProperty("url", "data:image/" + (id.getPath().endsWith("png") ? "png" : "jpeg") + ";base64," + Base64.getEncoder().encodeToString(bytes));
                result.addProperty("source", resource.sourcePackId());
            }
        } else throw new IllegalArgumentException("Unknown image kind");
        imageCache.put(key, result);
        return result;
    }
}
