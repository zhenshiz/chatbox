package com.zhenshiz.chatbox.editor;

import java.util.ArrayList;
import java.util.List;

/** Bounds and assembles an ordered editor message without exceeding Minecraft's C2S packet limit. */
public final class EditorFileChunks {
    public static final int CHUNK_SIZE = 8_000;
    public static final int MAX_CHUNKS = 256;
    private final StringBuilder text = new StringBuilder();
    private final int total;
    private int next;
    private final long created = System.nanoTime();

    /**
     * Creates an empty message assembler.
     * @param total the number of chunks, from 1 through 256
     * @throws IllegalArgumentException if the count is outside the supported range
     */
    public EditorFileChunks(int total) {
        if (total < 1 || total > MAX_CHUNKS) throw new IllegalArgumentException("Invalid editor message size");
        this.total = total;
    }

    /**
     * Appends the next chunk of this message.
     * @param index the zero-based, sequential chunk index
     * @param count the original chunk count
     * @param value the chunk containing at most 8,000 UTF-16 code units
     * @return the complete message, or {@code null} when more chunks are required
     * @throws IllegalArgumentException if ordering, size, count, or the 30-second deadline is invalid
     */
    public String append(int index, int count, String value) {
        if (index != next || count != total || next >= total || value.length() > CHUNK_SIZE || expired())
            throw new IllegalArgumentException("Invalid or expired editor message");
        text.append(value); next++;
        return next == total ? text.toString() : null;
    }

    /**
     * Returns whether this message has exceeded its 30-second assembly deadline.
     * @return whether the assembly deadline has elapsed
     */
    public boolean expired() { return System.nanoTime() - created > 30_000_000_000L; }

    /**
     * Splits a message without separating UTF-16 surrogate pairs.
     * @param text the serialized JSON message
     * @return the ordered chunks
     * @throws IllegalArgumentException if the message exceeds the supported size
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
