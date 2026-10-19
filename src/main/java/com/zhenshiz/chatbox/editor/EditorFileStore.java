package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Validates ChatBox data paths and atomically replaces JSON files inside a server-owned data pack. */
public final class EditorFileStore {
    public static final int MAX_BYTES = 1024 * 1024;
    private EditorFileStore() { }

    /**
     * Resolves a ChatBox JSON path within a data pack.
     * @param root the server-selected data pack directory
     * @param kind either {@code dialogue} or {@code theme}
     * @param id the fully qualified resource ID without a file extension
     * @return the contained path with no symbolic links in its existing descendants
     * @throws IOException if a symbolic link is present
     * @throws IllegalArgumentException if the ID or kind is invalid
     */
    public static Path resolve(Path root, String kind, String id) throws IOException {
        String folder = switch (kind) { case "dialogue" -> "dialogues"; case "theme" -> "theme"; default -> throw new IllegalArgumentException("Invalid ChatBox file kind"); };
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid ChatBox resource ID");
        String[] parts = id.split(":", 2);
        for (String segment : parts[1].split("/", -1))
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) throw new IllegalArgumentException("Invalid ChatBox resource path");
        if (parts[0].equals(".") || parts[0].equals("..")) throw new IllegalArgumentException("Invalid namespace");
        return contained(root, "data/" + parts[0] + "/chatbox/" + folder + "/" + parts[1] + ".json");
    }

    /**
     * Rejects paths outside the selected root and symbolic links below that root.
     * @param root the trusted, existing server directory
     * @param relative the relative path to inspect
     * @return the normalized path
     * @throws IOException if the target escapes the root or traverses a symbolic link
     */
    public static Path contained(Path root, String relative) throws IOException {
        Path base = root.toAbsolutePath().normalize();
        Path path = base.resolve(relative).normalize();
        if (!path.startsWith(base) || path.equals(base)) throw new IOException("Path is outside the data pack");
        Path cursor = base;
        if (Files.isSymbolicLink(cursor)) throw new IOException("Symbolic links cannot be edited");
        for (Path part : base.relativize(path)) {
            cursor = cursor.resolve(part);
            if (Files.isSymbolicLink(cursor)) throw new IOException("Symbolic links cannot be edited");
        }
        return path;
    }

    /**
     * Parses a bounded JSON object, retaining fields unknown to this editor.
     * @param content the UTF-8 JSON file contents
     * @return the parsed object
     * @throws IllegalArgumentException if the content is too large or is not a JSON object
     */
    public static JsonObject parse(String content) {
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new IllegalArgumentException("ChatBox file exceeds 1 MiB");
        var json = JsonParser.parseString(content);
        if (!json.isJsonObject()) throw new IllegalArgumentException("ChatBox file must be a JSON object");
        return json.getAsJsonObject();
    }

    /**
     * Hashes the source and exact file contents for optimistic concurrency checks.
     * @param source the selected source pack identifier
     * @param content the exact JSON text
     * @return the hexadecimal SHA-256 revision
     */
    public static String revision(String source, String content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((source + "\n" + content).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    /**
     * Atomically replaces a file; retains its previous contents beside it with a {@code .bak} suffix.
     * @param root the trusted data pack root
     * @param target the previously resolved file inside that root
     * @param content the validated JSON text
     * @throws IOException if path checks, backup, or atomic replacement fail
     */
    public static void write(Path root, Path target, String content) throws IOException {
        target = contained(root, root.toAbsolutePath().normalize().relativize(target.toAbsolutePath().normalize()).toString());
        Files.createDirectories(target.getParent());
        if (Files.exists(target)) {
            Path backup = contained(root, root.toAbsolutePath().normalize().relativize(target.resolveSibling(target.getFileName() + ".bak")).toString());
            Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        Path temporary = Files.createTempFile(target.getParent(), ".chatbox-editor-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}
