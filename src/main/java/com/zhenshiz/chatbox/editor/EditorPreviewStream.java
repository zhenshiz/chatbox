package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.client.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.KeyPromptRender;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.Util;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/** Streams completed Minecraft frames without queuing work behind a slow browser. */
final class EditorPreviewStream {
    private static final AtomicBoolean encoding = new AtomicBoolean();
    private static long nextFrame;
    private static long sequence;
    private static volatile String session;
    private static String revision;

    private EditorPreviewStream() { }

    static void start(String nextRevision) {
        session = java.util.UUID.randomUUID().toString();
        revision = nextRevision;
        nextFrame = 0;
    }

    static void stop() { session = null; }

    static void capture(ChatBoxScreen screen) {
        String current = session;
        long now = System.nanoTime();
        if (current == null || now < nextFrame || !EditorBridgeClient.canSendFrame() || !encoding.compareAndSet(false, true)) return;
        nextFrame = now + 125_000_000L;
        NativeImage image = null;
        try {
            Minecraft mc = Minecraft.getInstance();
            image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            double factor = Math.min(1, Math.min(1600.0 / image.getWidth(), 900.0 / image.getHeight()));
            if (factor < 1) {
                NativeImage resized = new NativeImage(Math.max(1, (int) (image.getWidth() * factor)), Math.max(1, (int) (image.getHeight() * factor)), false);
                try { image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), resized); }
                catch (RuntimeException error) { resized.close(); throw error; }
                image.close();
                image = resized;
            }
            JsonObject frame = new JsonObject();
            frame.addProperty("event", "preview.frame");
            frame.addProperty("revision", revision);
            frame.addProperty("sequence", ++sequence);
            frame.addProperty("width", image.getWidth());
            frame.addProperty("height", image.getHeight());
            frame.addProperty("guiWidth", mc.getWindow().getGuiScaledWidth());
            frame.addProperty("guiHeight", mc.getWindow().getGuiScaledHeight());
            JsonArray bounds = new JsonArray();
            ChatBoxTheme theme = ChatBoxUtil.chatBoxTheme;
            addBounds(bounds, "dialogBox", "", screen.dialogBox, theme.dialogBox);
            addBounds(bounds, "keyPrompt", "", screen.keyPromptRender, theme.keyPrompt);
            for (var portrait : screen.portraits) addBounds(bounds, "portrait", portrait.getId(), portrait, theme.portrait.get(portrait.getId()));
            for (int i = 0; i < screen.functionalButtons.size(); i++) addBounds(bounds, "functionalButton", Integer.toString(i), screen.functionalButtons.get(i), i < theme.functionalButton.size() ? theme.functionalButton.get(i) : null);
            for (int i = 0; i < screen.chatOptions.size(); i++) addBounds(bounds, "option", Integer.toString(i), screen.chatOptions.get(i), theme.option);
            frame.add("components", bounds);
            NativeImage captured = image;
            Util.ioPool().execute(() -> {
                try (captured) {
                    if (!current.equals(session)) return;
                    frame.addProperty("url", "data:image/png;base64," + Base64.getEncoder().encodeToString(captured.asByteArray()));
                    if (current.equals(session)) EditorBridgeClient.sendFrame(frame);
                } catch (Exception error) {
                    ChatBox.LOGGER.warn("Could not encode editor preview frame", error);
                    mc.execute(() -> { if (current.equals(session)) EditorBridgeClient.previewError(new IllegalStateException("Could not stream Minecraft preview", error)); });
                } finally { encoding.set(false); }
            });
        } catch (RuntimeException error) {
            if (image != null) image.close();
            encoding.set(false);
            EditorBridgeClient.previewError(error);
        }
    }

    private static void addBounds(JsonArray bounds, String kind, String key, AbstractComponent<?> component, ChatBoxTheme.Component draft) {
        if (component.hidden || component instanceof KeyPromptRender prompt && !Boolean.TRUE.equals(prompt.visible)) return;
        JsonObject rect = new JsonObject();
        rect.addProperty("kind", kind);
        rect.addProperty("key", key);
        rect.addProperty("x", component.x1());
        rect.addProperty("y", component.y1());
        rect.addProperty("width", component.x2() - component.x1());
        rect.addProperty("height", component.y2() - component.y1());
        rect.addProperty("angle", component.angle);
        rect.addProperty("order", component.renderOrder);
        if (draft != null) {
            rect.addProperty("positionX", draft.x == null ? 0 : draft.x);
            rect.addProperty("positionY", draft.y == null ? 0 : draft.y);
        }
        bounds.add(rect);
    }
}
