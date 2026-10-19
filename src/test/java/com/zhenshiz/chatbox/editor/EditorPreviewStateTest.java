package com.zhenshiz.chatbox.editor;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class EditorPreviewStateTest {
    @Test void clientPreviewDoesNotAffectIntegratedServerThread() throws InterruptedException {
        assertFalse(EditorPreviewState.isActive());
        EditorPreviewState.setActive(true);
        try {
            assertTrue(EditorPreviewState.isActive());
            AtomicBoolean serverPreview = new AtomicBoolean(true);
            Thread server = new Thread(() -> serverPreview.set(EditorPreviewState.isActive()));
            server.start();
            server.join();
            assertFalse(serverPreview.get());
        } finally {
            EditorPreviewState.setActive(false);
        }
        assertFalse(EditorPreviewState.isActive());
    }
}
