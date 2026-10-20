package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.client.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.KeyPromptRender;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.Util;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/** 推送完整的 Minecraft 渲染帧，避免浏览器接收缓慢时积压任务。 */
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
        float width = component.realWidth(), height = component.realHeight();
        float scale = component instanceof KeyPromptRender ? 1 : component.scale;
        rect.addProperty("x", component.realX() + width * (1 - scale) / 2);
        rect.addProperty("y", component.realY() + height * (1 - scale) / 2);
        rect.addProperty("width", width * scale);
        rect.addProperty("height", height * scale);
        rect.addProperty("angle", component instanceof KeyPromptRender || component instanceof Portrait.Entity ? 0 : component.angle);
        rect.addProperty("order", component.renderOrder);
        if (draft != null) {
            rect.addProperty("positionX", draft.x == null ? 0 : draft.x);
            rect.addProperty("positionY", draft.y == null ? 0 : draft.y);
            rect.addProperty("modelWidth", draft.width == null ? component.width : draft.width);
            rect.addProperty("modelHeight", draft.height == null ? component.height : draft.height);
            rect.addProperty("modelScale", draft.scale == null ? 1 : draft.scale);
            rect.addProperty("modelAngle", draft.angle == null ? 0 : draft.angle);
            rect.addProperty("realWidth", width);
            rect.addProperty("realHeight", height);
            rect.addProperty("renderScale", scale);
            rect.addProperty("alignX", component.alignX == AbstractComponent.AlignX.LEFT ? 0 : component.alignX == AbstractComponent.AlignX.CENTER ? 0.5 : 1);
            rect.addProperty("alignY", component.alignY == AbstractComponent.AlignY.TOP ? 0 : component.alignY == AbstractComponent.AlignY.CENTER ? 0.5 : 1);
            float widthUnit = component.widthReference.getLength(1);
            if (component.getClass().equals(Portrait.class)) widthUnit *= ChatBoxClient.conf.portraitWidthPercent / 100f;
            rect.addProperty("widthUnit", component instanceof Portrait.Entity ? 1 : widthUnit);
            // 选项共用主题尺寸，改变高度还会改变组内各行的排列位置。
            float heightOffsetUnit = component instanceof ChatOption option
                    ? (option.renderIndex - (ChatBoxUtil.chatBoxScreen.getRenderOptionCount() - 1)
                    * (component.alignY == AbstractComponent.AlignY.TOP ? 0 : component.alignY == AbstractComponent.AlignY.CENTER ? 0.5f : 1))
                    * Minecraft.getInstance().getWindow().getGuiScaledHeight() / 100f : 0;
            rect.addProperty("heightOffsetUnit", heightOffsetUnit);
            rect.addProperty("heightUnit", component instanceof Portrait.Entity ? 1 : component.heightReference.getLength(1));
            rect.addProperty("sizeMode", component instanceof KeyPromptRender ? "none" : (component instanceof Portrait.Item || component instanceof Portrait.Entity) ? "scale" : component instanceof Portrait.PlayerHead ? "square" : "box");
            rect.addProperty("rotatable", !(component instanceof KeyPromptRender || component instanceof Portrait.Entity));
        }
        bounds.add(rect);
    }
}
