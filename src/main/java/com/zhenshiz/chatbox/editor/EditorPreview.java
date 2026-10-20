package com.zhenshiz.chatbox.editor;

import com.google.gson.*;
import com.zhenshiz.chatbox.data.*;
import com.zhenshiz.chatbox.client.ChatBoxRender;
import com.zhenshiz.chatbox.client.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.client.screen.HistoricalDialogueScreen;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.component.KeyPromptRender;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.SoundUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** 隔离编辑器临时预览与玩家已加载的对话状态。 */
public final class EditorPreview {
    private static final ResourceLocation ID = ResourceLocation.parse("chatbox:editor_preview");
    private static Runnable restore;
    private static String revision = "";

    private EditorPreview() { }

    /**
     * 判断临时编辑器预览是否正在接管对话状态。
     *
     * @return 编辑器预览活动期间返回 {@code true}
     */
    public static boolean isActive() { return EditorPreviewState.isActive(); }

    /**
     * 获取活动预览中的翻译草稿。
     *
     * @param key 当前显示内容的翻译键
     * @return 草稿译文；需要使用游戏语言时返回 {@code null}
     */
    public static String translation(String key) { return isActive() ? EditorPreviewPack.translation(key) : null; }

    /** 恢复本次预览开始前保存的对话状态。 */
    public static void stop() {
        if (restore == null) return;
        EditorPreviewStream.stop();
        Runnable action = restore;
        restore = null;
        EditorPreviewState.setActive(false);
        try { action.run(); }
        finally { EditorBridgeClient.previewClosed(revision); revision = ""; }
    }

    static void captureFrame() {
        Minecraft mc = Minecraft.getInstance();
        if (isActive() && mc.screen instanceof PreviewScreen screen) EditorPreviewStream.capture(screen);
    }

    /**
     * 在当前预览版本中更新组件的位置及可选的宽高、缩放和角度。
     *
     * @param request 变换请求；宽高与缩放必须是有限的非负数，省略的属性保持不变
     * @return 空的成功响应
     * @throws IllegalArgumentException 组件不可用或变换参数无效时
     * @throws IllegalStateException 请求的预览版本已失效时
     */
    static JsonObject move(JsonObject request) {
        if (!isActive() || !revision.equals(request.get("revision").getAsString())
                || !(Minecraft.getInstance().screen instanceof PreviewScreen screen)) {
            throw new IllegalStateException("The editor preview has changed");
        }
        String kind = request.get("kind").getAsString();
        String key = request.has("key") ? request.get("key").getAsString() : "";
        float x = request.get("x").getAsFloat(), y = request.get("y").getAsFloat();
        if (!Float.isFinite(x) || !Float.isFinite(y)) throw new IllegalArgumentException("Invalid preview position");
        Float width = transformValue(request, "width", true), height = transformValue(request, "height", true);
        Float scale = transformValue(request, "scale", true), angle = transformValue(request, "angle", false);
        ChatBoxTheme theme = ChatBoxUtil.chatBoxTheme;
        ChatBoxTheme.Component draft = switch (kind) {
            case "dialogBox" -> theme.dialogBox;
            case "portrait" -> theme.portrait.get(key);
            case "option" -> theme.option;
            case "functionalButton" -> theme.functionalButton.get(Integer.parseInt(key));
            case "keyPrompt" -> theme.keyPrompt;
            default -> throw new IllegalArgumentException("Unknown preview component");
        };
        List<? extends AbstractComponent<?>> components = switch (kind) {
            case "dialogBox" -> List.of(screen.dialogBox);
            case "portrait" -> screen.portraits.stream().filter(p -> p.getId().equals(key)).toList();
            case "option" -> screen.chatOptions;
            case "functionalButton" -> List.of(screen.functionalButtons.get(Integer.parseInt(key)));
            case "keyPrompt" -> List.of(screen.keyPromptRender);
            default -> List.of();
        };
        if (draft == null || components.isEmpty()) throw new IllegalArgumentException("Preview component is unavailable");
        float dx = x - (draft.x == null ? 0 : draft.x), dy = y - (draft.y == null ? 0 : draft.y);
        Float scaleDelta = scale == null ? null : scale - (draft.scale == null ? 1 : draft.scale);
        Float angleDelta = angle == null ? null : angle - (draft.angle == null ? 0 : draft.angle);
        for (var component : components) {
            // 拖动期间保持组件当前的动画姿态，松开后再执行完整预览。
            if (component instanceof Portrait<?> portrait) portrait.setIsAnimation(false);
            float baseY = component instanceof ChatOption option ? option.originY : component.y;
            component.setPosition(component.x + dx, baseY + dy).setSize(width, height)
                    .setScale(scaleDelta == null ? null : Math.max(0, component.scale + scaleDelta))
                    .setAngle(angleDelta == null ? null : component.angle + angleDelta);
        }
        draft.x = x;
        draft.y = y;
        if (width != null) draft.width = width;
        if (height != null) draft.height = height;
        if (scale != null) draft.scale = scale;
        if (angle != null) draft.angle = angle;
        return new JsonObject();
    }

    private static Float transformValue(JsonObject request, String key, boolean nonNegative) {
        if (!request.has(key)) return null;
        float value = request.get(key).getAsFloat();
        if (!Float.isFinite(value) || nonNegative && value < 0) throw new IllegalArgumentException("Invalid preview " + key);
        return value;
    }

    static JsonObject show(JsonObject request) {
        EditorDebug.stop();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) throw new IllegalStateException("Enter a world before previewing a dialogue");
        JsonObject content = request.getAsJsonObject("content").deepCopy();
        JsonObject themeJson = request.has("theme") && request.get("theme").isJsonObject() ? request.getAsJsonObject("theme").deepCopy() : null;
        String kind = request.get("kind").getAsString();
        String group = request.has("group") ? request.get("group").getAsString() : "preview";
        int index = request.has("index") ? request.get("index").getAsInt() : 0;
        if (kind.equals("theme")) {
            themeJson = content;
            content = JsonParser.parseString("{\"dialogues\":{\"preview\":[{\"dialogBox\":{\"name\":\"ChatBox\",\"text\":\"chatbox.editor.sample\"}}]}}").getAsJsonObject();
            JsonObject sample = content.getAsJsonObject("dialogues").getAsJsonArray("preview").get(0).getAsJsonObject();
            JsonArray portraits = new JsonArray();
            if (themeJson.has("portrait")) themeJson.getAsJsonObject("portrait").keySet().forEach(portraits::add);
            sample.add("portrait", portraits);
            if (request.has("sample") && request.get("sample").isJsonObject()) {
                JsonObject settings = request.getAsJsonObject("sample");
                for (String field : List.of("name", "text")) if (settings.has(field)) sample.getAsJsonObject("dialogBox").add(field, settings.get(field));
                if (settings.has("options") && settings.get("options").isJsonArray()) {
                    JsonArray options = new JsonArray();
                    for (var text : settings.getAsJsonArray("options")) {
                        JsonObject option = new JsonObject();
                        option.add("text", text);
                        options.add(option);
                    }
                    sample.add("options", options);
                }
            }
            group = "preview";
            index = 0;
        } else if (!kind.equals("dialogue")) throw new IllegalArgumentException("Unknown preview kind");
        EditorPreviewSanitizer.sanitize(content);
        if (themeJson != null) EditorPreviewSanitizer.sanitize(themeJson);
        ChatBoxDialogues dialogue = ChatBoxUtil.GSON.fromJson(content, ChatBoxDialogues.class);
        var frames = dialogue.dialogues.get(group);
        if (frames == null || index < 0 || index >= frames.size()) throw new IllegalArgumentException("The selected dialogue group or frame is empty");
        if (kind.equals("dialogue")) {
            if (themeJson == null && dialogue.theme != null) {
                ResourceLocation sourceId = ResourceLocation.tryParse(dialogue.theme);
                ChatBoxTheme source = sourceId == null ? null : ChatBoxUtil.themeMap.get(sourceId);
                if (source != null) themeJson = ChatBoxUtil.GSON.toJsonTree(source).getAsJsonObject();
            }
            themeJson = EditorPreviewTheme.dialogue(themeJson);
        }
        JsonObject safeTheme = themeJson;
        EditorPreviewSanitizer.sanitize(safeTheme);
        ChatBoxTheme theme = ChatBoxUtil.GSON.fromJson(safeTheme, ChatBoxTheme.class).setDefaultValue();
        // 已解析的主题是独立副本，切换句子时不能重新加载游戏中的原主题。
        dialogue.theme = null;
        dialogue.isScreen = true;
        stop();
        revision = request.has("revision") ? request.get("revision").getAsString() : "";
        ChatBoxScreen oldScreen = ChatBoxUtil.chatBoxScreen;
        ChatBoxTheme oldTheme = ChatBoxUtil.chatBoxTheme;
        var oldHistory = ChatBoxUtil.historicalDialogue;
        var oldId = ChatBoxUtil.dialoguesResourceLocation;
        var oldGroup = ChatBoxUtil.group;
        var oldIndex = ChatBoxUtil.index;
        var oldDialogue = ChatBoxUtil.dialoguesMap.get(ID);
        var oldThemeId = ChatBoxUtil.themeResourceLocation;
        boolean oldMode = ChatBoxUtil.isScreen;
        boolean oldPauseOnLostFocus = mc.options.pauseOnLostFocus;
        boolean oldOpen = ChatBoxRender.isOpenChatBox;
        boolean oldRender = ChatBoxRender.shouldRender;
        var oldAnimations = new HashMap<>(ChatBoxUtil.animationMap);
        var previousScreen = mc.screen;
        String oldBgm = oldScreen.bgm;
        boolean resumeBgm = SoundUtil.isSoundActive(oldBgm);
        restore = () -> {
            if (mc.screen instanceof PreviewScreen) mc.setScreen(null);
            var previewScreen = ChatBoxUtil.chatBoxScreen;
            if (previewScreen.video != null) previewScreen.video.removeOnNext = true;
            previewScreen.setVideo(null).playBgm("");
            SoundUtil.stopSound(previewScreen.voice);
            ChatBoxUtil.chatBoxScreen = oldScreen;
            ChatBoxUtil.chatBoxTheme = oldTheme;
            ChatBoxUtil.historicalDialogue = oldHistory;
            ChatBoxUtil.dialoguesResourceLocation = oldId;
            ChatBoxUtil.group = oldGroup;
            ChatBoxUtil.index = oldIndex;
            ChatBoxUtil.themeResourceLocation = oldThemeId;
            ChatBoxUtil.isScreen = oldMode;
            mc.options.pauseOnLostFocus = oldPauseOnLostFocus;
            ChatBoxRender.isOpenChatBox = oldOpen;
            ChatBoxRender.shouldRender = oldRender;
            ChatBoxUtil.animationMap.clear();
            ChatBoxUtil.animationMap.putAll(oldAnimations);
            if (oldDialogue == null) ChatBoxUtil.dialoguesMap.remove(ID);
            else ChatBoxUtil.dialoguesMap.put(ID, oldDialogue);
            if (resumeBgm && mc.level != null) oldScreen.playBgm(oldBgm);
            if (previousScreen instanceof ChatBoxScreen && mc.level != null) mc.setScreen(previousScreen);
        };
        EditorPreviewState.setActive(true);
        try {
            SoundUtil.stopSound(oldBgm);
            ChatBoxUtil.chatBoxScreen = new PreviewScreen();
            ChatBoxUtil.historicalDialogue = new HistoricalDialogueScreen();
            ChatBoxUtil.chatBoxTheme = theme;
            ChatBoxUtil.animationMap.putAll(theme.customAnimation);
            ChatBoxUtil.dialoguesMap.put(ID, dialogue);
            ChatBoxUtil.dialoguesResourceLocation = null;
            ChatBoxUtil.isScreen = true;
            ChatBoxRender.shouldRender = false;
            ChatBoxUtil.chatBoxScreen.setDialogBox(theme.dialogBox.setDialogBoxTheme(ChatBoxUtil.chatBoxScreen.dialogBox))
                    .setFunctionalButtons(ChatBoxTheme.setButtonTheme(theme.functionalButton))
                    .setKeyPromptRender(theme.keyPrompt.setKeyPromptTheme(new PreviewKeyPrompt()));
            dialogue.isEsc = true;
            dialogue.isPause = false;
            dialogue.isHistoricalSkip = false;
            ChatBoxUtil.skipDialogues(ID, group, index);
            if (request.has("stream") && request.get("stream").getAsBoolean()) {
                mc.options.pauseOnLostFocus = false;
                ((PreviewScreen) ChatBoxUtil.chatBoxScreen).streaming = true;
                EditorPreviewStream.start(request.get("revision").getAsString());
            }
            JsonObject result = new JsonObject();
            result.addProperty("group", group);
            result.addProperty("index", index);
            return result;
        } catch (RuntimeException error) { stop(); throw error; }
    }

    private static final class PreviewScreen extends ChatBoxScreen {
        private boolean streaming;
        @Override protected boolean showKeyPromptInScreen() { return true; }
        @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            try { super.render(graphics, mouseX, mouseY, partialTick); }
            catch (RuntimeException error) { EditorBridgeClient.previewError(error); }
        }
        @Override public void tick() {
            try { super.tick(); }
            catch (RuntimeException error) { EditorBridgeClient.previewError(error); }
        }
        @Override public boolean mouseClicked(double x, double y, int button) {
            if (streaming) return true;
            try { return super.mouseClicked(x, y, button); }
            catch (RuntimeException error) { EditorBridgeClient.previewError(error); return true; }
        }
        @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
            if (streaming) { if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) stop(); return true; }
            try { return super.keyPressed(key, scanCode, modifiers); }
            catch (RuntimeException error) { EditorBridgeClient.previewError(error); return true; }
        }
        @Override public void onClose() { stop(); }
        @Override public void removed() { if (isActive()) stop(); }
        @Override public boolean isPauseScreen() { return false; }
    }

    private static final class PreviewKeyPrompt extends KeyPromptRender {
        @Override public float realWidth() { return contentWidth(); }
        @Override public float realHeight() { return contentHeight(); }
    }
}
