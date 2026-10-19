package com.zhenshiz.chatbox.editor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class EditorFileStoreTest {
    @TempDir Path root;

    @Test void resolvesOnlyChatBoxResourcesInsideTheSelectedPack() throws Exception {
        assertEquals(root.resolve("data/test/chatbox/theme/menu/default.json"), EditorFileStore.resolve(root, "theme", "test:menu/default"));
        for (String id : new String[]{"test:../../outside", "../test:file", "test:/outside", "test:a//b", "test:a/./b", "test:a/../b"})
            assertThrows(IllegalArgumentException.class, () -> EditorFileStore.resolve(root, "theme", id), id);
        assertThrows(IllegalArgumentException.class, () -> EditorFileStore.resolve(root, "assets", "test:default"));
        assertThrows(java.io.IOException.class, () -> EditorFileStore.contained(root, "../outside"));
    }

    @Test void rejectsSymlinkDirectoriesAndBackups() throws Exception {
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path pack = Files.createDirectory(root.resolve("pack"));
        Files.createSymbolicLink(pack.resolve("data"), outside);
        assertThrows(java.io.IOException.class, () -> EditorFileStore.resolve(pack, "theme", "test:default"));
        Files.delete(pack.resolve("data"));
        Path file = EditorFileStore.resolve(pack, "theme", "test:default");
        EditorFileStore.write(pack, file, "{}");
        Files.createSymbolicLink(file.resolveSibling(file.getFileName() + ".bak"), outside.resolve("stolen"));
        assertThrows(java.io.IOException.class, () -> EditorFileStore.write(pack, file, "{\"new\":1}"));
        assertFalse(Files.exists(outside.resolve("stolen")));
        assertEquals("{}", Files.readString(file));
    }

    @Test void backsUpTheExactPreviousContentAndKeepsUnknownJsonFields() throws Exception {
        Path file = EditorFileStore.resolve(root, "dialogue", "test:menu/start");
        String first = "{\"dialogues\":{},\"extension\":\"中文😀\"}";
        EditorFileStore.write(root, file, first);
        EditorFileStore.write(root, file, "{\"dialogues\":{}}");
        assertEquals(first, Files.readString(file.resolveSibling("start.json.bak")));
        assertEquals("中文😀", EditorFileStore.parse(first).get("extension").getAsString());
        assertNotEquals(EditorFileStore.revision("mod/test", first), EditorFileStore.revision("file/override", first));
        assertNotEquals(EditorFileStore.revision("mod/test", first), EditorFileStore.revision("mod/test", first + "\n"));
        assertThrows(IllegalArgumentException.class, () -> EditorFileStore.parse("[]"));
        assertThrows(IllegalArgumentException.class, () -> EditorFileStore.parse("{\"huge\":\"" + "中".repeat(EditorFileStore.MAX_BYTES / 3) + "\"}"));
    }

    @Test void chunksRoundTripUnicodeAndRejectReorderingOrOversize() {
        String content = "x".repeat(7999) + "😀中文".repeat(6000);
        var parts = EditorFileChunks.split(content);
        EditorFileChunks chunks = new EditorFileChunks(parts.size());
        String joined = null;
        for (int i = 0; i < parts.size(); i++) {
            assertFalse(Character.isHighSurrogate(parts.get(i).charAt(parts.get(i).length() - 1)));
            joined = chunks.append(i, parts.size(), parts.get(i));
        }
        assertEquals(content, joined);
        assertThrows(IllegalArgumentException.class, () -> new EditorFileChunks(257));
        assertThrows(IllegalArgumentException.class, () -> new EditorFileChunks(2).append(1, 2, "x"));
        assertThrows(IllegalArgumentException.class, () -> new EditorFileChunks(1).append(0, 1, "x".repeat(8001)));
    }
}
