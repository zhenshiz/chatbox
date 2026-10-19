package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Supplies session-local image resources and preview-only draft translations. */
final class EditorPreviewPack {
    private static Path root;
    private static final Map<String, String> hashes = new HashMap<>();
    private static final Map<String, Map<String, String>> draftTranslations = new HashMap<>();
    private static boolean dirty;

    private EditorPreviewPack() { }

    static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        try {
            if (root == null) {
                root = Files.createTempDirectory("chatbox-editor-");
                Runtime.getRuntime().addShutdownHook(new Thread(EditorPreviewPack::delete, "ChatBox preview cleanup"));
            }
            int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
            Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":" + format + ",\"description\":\"ChatBox editor preview\"}}");
            event.addRepositorySource(consumer -> {
                Pack pack = Pack.readMetaAndCreate(new PackLocationInfo("chatbox-editor-preview", Component.literal("ChatBox editor preview"),
                                PackSource.BUILT_IN, Optional.empty()), new PathPackResources.PathResourcesSupplier(root),
                        PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.TOP, true));
                if (pack != null) consumer.accept(pack);
            });
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    static void upload(String id, String encoded) throws Exception {
        ResourceLocation location = ResourceLocation.parse(id);
        if (!location.getPath().startsWith("textures/") || !location.getPath().matches(".*\\.(png|jpg|jpeg)$")) {
            throw new IllegalArgumentException("Only texture images can be uploaded");
        }
        path(location);
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length > 8 * 1024 * 1024) throw new IllegalArgumentException("Image exceeds 8 MiB");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Invalid image");
            var reader = readers.next();
            try {
                reader.setInput(input);
                if (reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096) throw new IllegalArgumentException("Image exceeds 4096 pixels");
                var image = reader.read(0);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                write(location, output.toByteArray());
            } finally { reader.dispose(); }
        }
    }

    static CompletableFuture<Void> commit(Set<String> images, JsonObject translations) throws Exception {
        Set<String> keep = new HashSet<>(images);
        if (!hashes.keySet().containsAll(keep)) throw new IllegalArgumentException("Upload referenced images before committing preview resources");
        Map<String, Map<String, String>> nextTranslations = new HashMap<>();
        for (var language : translations.entrySet()) {
            if (!language.getKey().matches("[a-z_]+")) throw new IllegalArgumentException("Invalid language code");
            byte[] bytes = language.getValue().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (bytes.length > 2 * 1024 * 1024) throw new IllegalArgumentException("Translation draft exceeds 2 MiB");
            Map<String, String> entries = new HashMap<>();
            language.getValue().getAsJsonObject().entrySet().forEach(entry -> entries.put(entry.getKey(), entry.getValue().getAsString()));
            nextTranslations.put(language.getKey(), entries);
        }
        draftTranslations.clear();
        draftTranslations.putAll(nextTranslations);
        for (String id : new HashSet<>(hashes.keySet())) {
            if (!keep.contains(id)) {
                Files.deleteIfExists(path(ResourceLocation.parse(id)));
                hashes.remove(id);
                dirty = true;
            }
        }
        if (!dirty) return CompletableFuture.completedFuture(null);
        dirty = false;
        return Minecraft.getInstance().reloadResourcePacks().whenComplete((ignored, error) -> { if (error != null) dirty = true; });
    }

    static String translation(String key) {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        String value = draftTranslations.getOrDefault(language, Map.of()).get(key);
        if (value == null && !net.minecraft.locale.Language.getInstance().has(key)) value = draftTranslations.getOrDefault("en_us", Map.of()).get(key);
        return value;
    }

    private static void write(ResourceLocation id, byte[] bytes) throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (hash.equals(hashes.get(id.toString()))) return;
        long total;
        try (var paths = Files.walk(root)) {
            total = paths.filter(Files::isRegularFile).mapToLong(p -> { try { return Files.size(p); } catch (IOException e) { return 0; } }).sum();
        }
        Path file = path(id);
        long previousSize = Files.exists(file) ? Files.size(file) : 0;
        if (total - previousSize + bytes.length > 64 * 1024 * 1024) throw new IllegalArgumentException("Preview resources exceed 64 MiB");
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
        hashes.put(id.toString(), hash);
        dirty = true;
    }

    private static Path path(ResourceLocation id) {
        if (root == null) throw new IllegalStateException("Preview resource pack is unavailable");
        Path file = root.resolve("assets").resolve(id.getNamespace()).resolve(id.getPath()).normalize();
        if (!file.startsWith(root.resolve("assets")) || id.getPath().contains("..")) throw new IllegalArgumentException("Invalid resource path");
        return file;
    }

    private static void delete() {
        if (root == null) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException ignored) { }
    }
}
