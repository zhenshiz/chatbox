package com.zhenshiz.chatbox.editor;

import com.google.gson.*;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.platform.Lighting;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
//? >= 1.21
/*import net.minecraft.core.component.DataComponents;*/
import net.minecraft.core.registries.*;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
//? >= 1.21
/*import net.minecraft.world.item.component.CustomModelData;*/
import org.joml.Matrix4f;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.BiConsumer;

/**
 * 向已配对的网站开放当前客户端资源与临时对话预览。
 *
 * <p>游戏访问在客户端线程上执行，桥接服务通过
 * {@code /chatbox editor start} 显式启动，同一时间只允许一个编辑器认证连接。
 */
public final class EditorBridgeClient {
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

    /** 在客户端模组初始化时注册桥接回调。 */
    public static void install() { EditorClientEvents.register(); }

    /**
     * 注册本机桥接服务的启动与关闭命令。
     *
     * @param <S> 加载器提供的客户端命令来源类型
     * @param dispatcher 客户端命令分派器
     * @param success 向命令来源输出提示的回调
     * @param failure 向命令来源输出错误的回调
     */
    public static <S> void commands(CommandDispatcher<S> dispatcher, BiConsumer<S, Component> success, BiConsumer<S, Component> failure) {
        dispatcher.register(LiteralArgumentBuilder.<S>literal("chatbox")
                .then(LiteralArgumentBuilder.<S>literal("editor")
                .then(LiteralArgumentBuilder.<S>literal("start").executes(context -> {
                    int attempt = ++startAttempt;
                    var connection = Minecraft.getInstance().getConnection();
                    authorize().whenComplete((permission, permissionFailure) -> {
                        if (attempt != startAttempt || connection != Minecraft.getInstance().getConnection()) return;
                        try {
                            if (permissionFailure != null) throw new IllegalStateException(permissionFailure.getMessage(), permissionFailure);
                            serverSession = permission.get("serverSession").getAsString();
                            if (server == null) {
                                server = new EditorSocketServer(EditorBridgeClient::dispatch, () -> Minecraft.getInstance().execute(EditorBridgeClient::disconnected));
                                port = server.start(0);
                                lastLanguage = Language.getInstance();
                                lastLevel = Minecraft.getInstance().level;
                            }
                            String address = "ws://127.0.0.1:" + port + "/chatbox-editor";
                            String token = server.token();
                            success.accept(context.getSource(), Component.translatable("chatbox.editor.connection", copyable(address)));
                            success.accept(context.getSource(), Component.translatable("chatbox.editor.token", copyable(token)));
                            success.accept(context.getSource(), Component.translatable("chatbox.editor.instructions"));
                        } catch (Exception error) {
                            closeBridge();
                            failure.accept(context.getSource(), Component.translatable("chatbox.editor.error", error.getMessage()));
                        }
                    });
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<S>literal("stop").executes(context -> {
                    closeBridge();
                    success.accept(context.getSource(), Component.translatable("chatbox.editor.stopped"));
                    return 1;
                }))));
    }

    private static Component copyable(String value) {
        return Component.literal(value).withStyle(style -> style
                .withColor(net.minecraft.ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chatbox.editor.copy"))));
    }

    /**
     * 客户端语言、资源或世界变化后，使网页缓存失效。

     */
    public static void tick() {
        if (server == null || ++ticks % 20 != 0) return;
        Minecraft mc = Minecraft.getInstance();
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
        JsonObject request = new JsonObject(); request.addProperty("op", "authorize");
        return EditorFileClient.request(request);
    }

    private static void closeBridge() {
        ++startAttempt;
        if (server != null) server.close();
        server = null; serverSession = null; checkingPermission = false;
        disconnected();
    }

    /** 游戏连接关闭时结束编辑器会话，并取消待完成的服务端操作。
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
        Minecraft.getInstance().execute(() -> {
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
            authorize().whenComplete((permission, permissionFailure) -> {
            if (server != expected || expected == null) { result.completeExceptionally(new IllegalStateException("游戏连接已变化，请重新连接编辑器")); return; }
            if (permissionFailure != null || !permission.get("serverSession").getAsString().equals(serverSession)) {
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

    /** 只采集属于临时编辑器界面的完整渲染帧。 */
    public static void renderFrame() { EditorPreview.captureFrame(); }

    private static JsonObject snapshot() {
        Minecraft mc = Minecraft.getInstance();
        JsonObject result = new JsonObject();
        result.addProperty("protocol", EditorSocketServer.PROTOCOL);
        result.addProperty("previewStream", true);
        result.addProperty("previewMove", true);
        result.addProperty("previewTransform", true);
        result.addProperty("dialogueDebug", true);
        result.addProperty("minecraft", net.minecraft.SharedConstants.getCurrentVersion().getName());
        result.addProperty("chatbox", ChatBox.PLATFORM.getModVersion(ChatBox.MOD_ID));
        result.addProperty("language", mc.getLanguageManager().getSelected());
        result.addProperty("inWorld", mc.level != null);
        result.add("translations", ChatBoxUtil.GSON.toJsonTree(translations(mc)));
        JsonObject registries = new JsonObject();
        registries.add("ITEM", entries(BuiltInRegistries.ITEM, item -> item.getDescription().getString(), true));
        registries.add("BLOCK", entries(BuiltInRegistries.BLOCK, block -> block.getName().getString(), true));
        registries.add("EFFECT", entries(BuiltInRegistries.MOB_EFFECT, effect -> effect.getDisplayName().getString(), false));
        registries.add("ATTRIBUTE", entries(BuiltInRegistries.ATTRIBUTE, attribute -> Component.translatable(attribute.getDescriptionId()).getString(), false));
        registries.add("ENTITY", entries(BuiltInRegistries.ENTITY_TYPE, entity -> entity.getDescription().getString(), false));
        JsonArray living = new JsonArray();
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            if (isLiving(type, mc)) living.add(option(type.getDescription().getString(), BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        }
        registries.add("LIVING_ENTITY", living);
        registries.add("SOUND", entries(BuiltInRegistries.SOUND_EVENT, sound -> sound.getLocation().toString(), false));
        registries.add("PARTICLE", entries(BuiltInRegistries.PARTICLE_TYPE, particle -> BuiltInRegistries.PARTICLE_TYPE.getKey(particle).toString(), false));
        registries.add("FLUID", entries(BuiltInRegistries.FLUID, fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString(), false));
        addTags(registries, "ITEM_TAG", BuiltInRegistries.ITEM);
        addTags(registries, "BLOCK_TAG", BuiltInRegistries.BLOCK);
        addTags(registries, "ENTITY_TAG", BuiltInRegistries.ENTITY_TYPE);
        if (mc.level != null) {
            //? >= 1.21 {
            /*var enchantments = mc.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            registries.add("ENCHANTMENT", entries(enchantments, enchantment -> enchantment.description().getString(), false));
            *///?} else {
            var enchantments = BuiltInRegistries.ENCHANTMENT;
            registries.add("ENCHANTMENT", entries(enchantments, enchantment -> Component.translatable(enchantment.getDescriptionId()).getString(), false));
            //?}
            addTags(registries, "ENCHANTMENT_TAG", enchantments);
            var biomes = mc.level.registryAccess().registryOrThrow(Registries.BIOME);
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

    private static boolean isLiving(net.minecraft.world.entity.EntityType<?> type, Minecraft client) {
        if (client.level == null) return false;
        try { return type.create(client.level) instanceof LivingEntity; }
        catch (RuntimeException error) { return false; }
    }

    private static Map<String, String> translations(Minecraft client) {
        Map<String, String> result = new HashMap<>();
        List<String> languages = new ArrayList<>(List.of("en_us"));
        String selected = client.getLanguageManager().getSelected();
        if (!selected.equals("en_us")) languages.add(selected);
        for (String language : languages) {
            for (String namespace : client.getResourceManager().getNamespaces()) {
                for (var resource : client.getResourceManager().getResourceStack(ChatBox.parseId(namespace + ":lang/" + language + ".json"))) {
                    try (var input = resource.open()) { Language.loadFromJson(input, result::put); }
                    catch (IOException | RuntimeException error) { ChatBox.LOGGER.debug("无法读取编辑器翻译资源", error); }
                }
            }
        }
        return result;
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
        registry.getTagNames().sorted(Comparator.comparing(tag -> tag.location().toString()))
                .forEach(tag -> tags.add(option("#" + tag.location(), "#" + tag.location())));
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
        ResourceLocation id = ChatBox.parseId(request.get("resourceId").getAsString());
        int customData = request.has("customModelData") ? request.get("customModelData").getAsInt() : 0;
        String key = kind + ":" + id + ":" + customData;
        if (imageCache.containsKey(key)) return imageCache.get(key);
        JsonObject result;
        if (kind.equals("item")) result = renderItem(id, customData);
        else if (kind.equals("texture")) {
            if (!id.getPath().startsWith("textures/") || !id.getPath().matches(".*\\.(png|jpg|jpeg)$")) throw new IllegalArgumentException("Invalid texture resource");
            var resource = Minecraft.getInstance().getResourceManager().getResource(id).orElseThrow(() -> new IllegalArgumentException("Texture not found: " + id));
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

    private static JsonObject renderItem(ResourceLocation id, int customData) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalArgumentException("Unknown item: " + id);
        var stack = BuiltInRegistries.ITEM.get(id).getDefaultInstance();
        if (customData != 0) {
            //? >= 1.21 {
            /*stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customData));
            *///?} else {
            stack.getOrCreateTag().putInt("CustomModelData", customData);
            //?}
        }
        TextureTarget target = new TextureTarget(64, 64, true, Minecraft.ON_OSX);
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var oldSorting = RenderSystem.getVertexSorting();
        var modelView = RenderSystem.getModelViewStack();
        float[] oldColor = RenderSystem.getShaderColor().clone();
        //? >= 1.21 {
        /*modelView.pushMatrix();
        *///?} else {
        modelView.pushPose();
        //?}
        try {
            target.setClearColor(0, 0, 0, 0);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 64, 64, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            //? >= 1.21 {
            /*modelView.translation(0, 0, -11000);
            *///?} else {
            modelView.setIdentity();
            modelView.translate(0, 0, -11000);
            //?}
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            Lighting.setupFor3DItems();
            GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            graphics.pose().scale(4, 4, 4);
            graphics.renderItem(stack, 0, 0);
            graphics.flush();
            try (NativeImage image = new NativeImage(64, 64, false)) {
                RenderSystem.bindTexture(target.getColorTextureId());
                image.downloadTexture(0, false);
                image.flipY();
                JsonObject result = new JsonObject();
                result.addProperty("url", "data:image/png;base64," + Base64.getEncoder().encodeToString(image.asByteArray()));
                result.addProperty("width", 64);
                result.addProperty("height", 64);
                return result;
            }
        } finally {
            //? >= 1.21 {
            /*modelView.popMatrix();
            *///?} else {
            modelView.popPose();
            //?}
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            target.destroyBuffers();
            mc.getMainRenderTarget().bindWrite(true);
        }
    }
}
