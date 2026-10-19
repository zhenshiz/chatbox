package com.zhenshiz.chatbox.editor;

/** Tracks preview ownership without loading client classes on a dedicated server. */
public final class EditorPreviewState {
    private static volatile Thread owner;

    private EditorPreviewState() { }

    /**
     * Reports whether the current thread is executing the temporary client preview.
     *
     * @return {@code true} on the preview thread, never on the integrated server thread
     */
    public static boolean isActive() { return Thread.currentThread() == owner; }

    static void setActive(boolean active) { owner = active ? Thread.currentThread() : null; }
}
