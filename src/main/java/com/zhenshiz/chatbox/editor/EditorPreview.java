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

/** Isolates temporary editor dialogue state from the player's loaded dialogue state. */
public final class EditorPreview {
    private static final ResourceLocation ID = ResourceLocation.parse("chatbox:editor_preview");
    private static Runnable restore;
    private static String revision = "";

    private EditorPreview() { }

    /**
     * Reports whether a temporary editor preview currently owns the dialogue state.
     *
     * @return {@code true} while an editor preview is active
     */
    public static boolean isActive() { return EditorPreviewState.isActive(); }

    /**
     * Resolves a draft translation for the active preview.
     *
     * @param key the translation key being displayed
     * @return the draft text, or {@code null} when the game language should be used
     */
    public static String translation(String key) { return isActive() ? EditorPreviewPack.translation(key) : null; }

    /** Restores the dialogue state captured before the current preview. */
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

    // Position edits reuse the current screen; rebuilding it for every mouse move restarts animations.
    static JsonObject move(JsonObject request) {
        if (!isActive() || !revision.equals(request.get("revision").getAsString())
                || !(Minecraft.getInstance().screen instanceof PreviewScreen screen)) {
            throw new IllegalStateException("The editor preview has changed");
        }
        String kind = request.get("kind").getAsString();
        String key = request.has("key") ? request.get("key").getAsString() : "";
        float x = request.get("x").getAsFloat(), y = request.get("y").getAsFloat();
        if (!Float.isFinite(x) || !Float.isFinite(y)) throw new IllegalArgumentException("Invalid preview position");
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
        for (var component : components) {
            // Hold the dragged component at its visible animation pose until the final full preview.
            if (component instanceof Portrait<?> portrait) portrait.setIsAnimation(false);
            float baseY = component instanceof ChatOption option ? option.originY : component.y;
            component.setPosition(component.x + dx, baseY + dy);
        }
        draft.x = x;
        draft.y = y;
        return new JsonObject();
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
        // The resolved theme is an isolated copy; frame navigation must not reload the game copy.
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
