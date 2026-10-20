package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** 校验 ChatBox 数据路径，并在服务端管理的数据包内原子替换 JSON 文件。 */
public final class EditorFileStore {
    public static final int MAX_BYTES = 1024 * 1024;
    private EditorFileStore() { }

    /**
     * 解析数据包内的 ChatBox JSON 路径。
     * @param root 服务端选定的数据包目录
     * @param kind 资源类型，仅支持 {@code dialogue} 或 {@code theme}
     * @param id 含命名空间且不含扩展名的资源 ID
     * @return 限定在数据包内且现有子路径不含符号链接的路径
     * @throws IOException 路径中存在符号链接时抛出
     * @throws IllegalArgumentException 资源 ID 或类型无效时抛出
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
     * 拒绝越过根目录或经过根目录下符号链接的路径。
     * @param root 已存在且可信的服务端目录
     * @param relative 待校验的相对路径
     * @return 规范化后的路径
     * @throws IOException 目标越过根目录或经过符号链接时抛出
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
     * 解析受大小限制的 JSON 对象，保留编辑器尚不识别的字段。
     * @param content UTF-8 编码的 JSON 文件内容
     * @return 解析后的对象
     * @throws IllegalArgumentException 内容超出大小限制或不是 JSON 对象时抛出
     */
    public static JsonObject parse(String content) {
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new IllegalArgumentException("ChatBox file exceeds 1 MiB");
        var json = JsonParser.parseString(content);
        if (!json.isJsonObject()) throw new IllegalArgumentException("ChatBox file must be a JSON object");
        return json.getAsJsonObject();
    }

    /**
     * 根据资源来源与文件原文生成摘要，用于乐观并发校验。
     * @param source 选定来源资源包的标识符
     * @param content 未经改写的 JSON 原文
     * @return 十六进制 SHA-256 版本摘要
     */
    public static String revision(String source, String content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((source + "\n" + content).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    /**
     * 原子替换文件，并将原内容以 {@code .bak} 后缀备份在同目录。
     * @param root 可信的数据包根目录
     * @param target 已解析且位于根目录内的目标文件
     * @param content 已通过校验的 JSON 文本
     * @throws IOException 路径校验、备份或原子替换失败时抛出
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
