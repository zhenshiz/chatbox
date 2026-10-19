package com.zhenshiz.chatbox.editor;

import java.util.ArrayList;
import java.util.List;

/** 限制并组装有序的编辑器消息，避免超过 Minecraft 客户端到服务端的数据包上限。 */
public final class EditorFileChunks {
    public static final int CHUNK_SIZE = 8_000;
    public static final int MAX_CHUNKS = 256;
    private final StringBuilder text = new StringBuilder();
    private final int total;
    private int next;
    private final long created = System.nanoTime();

    /**
     * 创建空的消息组装器。
     * @param total 分片总数，范围为 1 到 256
     * @throws IllegalArgumentException 分片总数超出范围时抛出
     */
    public EditorFileChunks(int total) {
        if (total < 1 || total > MAX_CHUNKS) throw new IllegalArgumentException("Invalid editor message size");
        this.total = total;
    }

    /**
     * 按顺序追加消息的下一个分片。
     * @param index 从 0 开始且连续递增的分片索引
     * @param count 原始分片总数
     * @param value 最多包含 8000 个 UTF-16 代码单元的分片
     * @return 完整消息；仍需等待分片时返回 {@code null}
     * @throws IllegalArgumentException 分片顺序、大小、数量无效或超过 30 秒组装期限时抛出
     */
    public String append(int index, int count, String value) {
        if (index != next || count != total || next >= total || value.length() > CHUNK_SIZE || expired())
            throw new IllegalArgumentException("Invalid or expired editor message");
        text.append(value); next++;
        return next == total ? text.toString() : null;
    }

    /**
     * 判断消息是否超过 30 秒的组装期限。
     * @return 是否超过组装期限
     */
    public boolean expired() { return System.nanoTime() - created > 30_000_000_000L; }

    /**
     * 拆分消息，避免将 UTF-16 代理项对分开。
     * @param text 已序列化的 JSON 消息
     * @return 有序的分片列表
     * @throws IllegalArgumentException 消息超出支持的大小时抛出
     */
    public static List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < text.length();) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
            chunks.add(text.substring(start, end)); start = end;
        }
        if (chunks.isEmpty()) chunks.add("");
        if (chunks.size() > MAX_CHUNKS) throw new IllegalArgumentException("Editor message is too large");
        return chunks;
    }
}
