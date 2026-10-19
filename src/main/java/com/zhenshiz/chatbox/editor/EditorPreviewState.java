package com.zhenshiz.chatbox.editor;

/** 记录预览状态，避免在独立服务端加载客户端类。 */
public final class EditorPreviewState {
    private static volatile Thread owner;

    private EditorPreviewState() { }

    /**
     * 判断当前线程是否正在执行临时客户端预览。
     *
     * @return 仅预览线程返回 {@code true}，集成服务端线程始终返回 {@code false}
     */
    public static boolean isActive() { return Thread.currentThread() == owner; }

    static void setActive(boolean active) { owner = active ? Thread.currentThread() : null; }
}
