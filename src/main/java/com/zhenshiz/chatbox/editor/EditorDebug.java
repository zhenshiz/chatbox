package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.client.ChatBoxRender;
import com.zhenshiz.chatbox.client.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.client.screen.HistoricalDialogueScreen;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.SoundUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** 通过正常的客户端与服务端事件链路调试未保存的对话。 */
public final class EditorDebug {
    private static Runnable restore;
    private static String session = "";
    private static String pendingSession = "";
    private static ResourceLocation resourceId;
    private static int version, generation, sequence;

    private EditorDebug() { }

    /**
     * 判断当前对话状态是否由交互调试会话接管。
     * @return 是否存在活动的交互调试会话
     */
    public static boolean isActive() { return restore != null; }

    static CompletableFuture<JsonObject> start(JsonObject request) {
        ChatBoxDialogues draft = parse(request);
        String group = request.get("group").getAsString();
        int index = request.get("index").getAsInt();
        if (!draft.dialogues.containsKey(group) || index < 0 || index >= draft.dialogues.get(group).size())
            throw new IllegalArgumentException("请选择有效的对话节点");
        EditorPreview.stop();
        stop();
        int attempt = ++generation;
        pendingSession = request.get("session").getAsString();
        Object connection = Minecraft.getInstance().getConnection();
        JsonObject operation = operation(request, "debug.start");
        return EditorFileClient.request(operation).thenApply(result -> {
            if (attempt != generation || connection != Minecraft.getInstance().getConnection()) {
                operation.addProperty("op", "debug.stop");
                EditorFileClient.request(operation);
                throw new IllegalStateException("调试启动已取消");
            }
            install(request, draft);
            pendingSession = "";
            return result;
        });
    }

    static CompletableFuture<JsonObject> update(JsonObject request) {
        requireSession(request);
        ChatBoxDialogues draft = parse(request);
        int attempt = generation;
        return EditorFileClient.request(operation(request, "debug.update")).thenApply(result -> {
            if (attempt != generation) throw new IllegalStateException("调试会话已结束");
            requireSession(request);
            ChatBoxDialogues previous = ChatBoxUtil.dialoguesMap.put(resourceId, draft);
            version = request.get("version").getAsInt();
            // 原地更新画面，避免重新进入对话时重放指令、声音和开始事件。
            if (resourceId.equals(ChatBoxUtil.dialoguesResourceLocation)) {
                var before = frame(previous, ChatBoxUtil.group, ChatBoxUtil.index);
                var after = frame(draft, ChatBoxUtil.group, ChatBoxUtil.index);
                if (after == null) { stop(); throw new IllegalStateException("运行中的对话已被删除，请重新跳转"); }
                if (before == null || !Objects.equals(before.dialogBox.text, after.dialogBox.text))
                    ChatBoxUtil.chatBoxScreen.dialogBox.setText(after.dialogBox.text);
                if (before == null || !Objects.equals(before.dialogBox.name, after.dialogBox.name))
                    ChatBoxUtil.chatBoxScreen.dialogBox.setName(after.dialogBox.name);
                refreshOptions(before, after);
                refreshPortraits(previous, draft);
            }
            position();
            return result;
        });
    }

    private static void refreshOptions(ChatBoxDialogues.Dialogues before, ChatBoxDialogues.Dialogues after) {
        var old = before == null || before.options == null ? List.<ChatBoxDialogues.Dialogues.Option>of() : before.options;
        var next = after.options == null ? List.<ChatBoxDialogues.Dialogues.Option>of() : after.options;
        if (ChatBoxUtil.GSON.toJsonTree(old).equals(ChatBoxUtil.GSON.toJsonTree(next))) return;
        var rendered = ChatBoxUtil.chatBoxScreen.chatOptions;
        var replacements = after.setChatOptionDialogues();
        var used = new boolean[old.size()];
        for (int i = 0; i < next.size(); i++) {
            var option = next.get(i);
            int match = -1;
            for (int j = 0; j < Math.min(old.size(), rendered.size()); j++) {
                if (!used[j] && ChatBoxUtil.GSON.toJsonTree(old.get(j)).equals(ChatBoxUtil.GSON.toJsonTree(option))) { match = j; break; }
            }
            if (match < 0 && i < old.size() && i < rendered.size() && !used[i]
                    && optionBehavior(old.get(i)).equals(optionBehavior(option))) match = i;
            if (match >= 0) {
                used[match] = true;
                replacements.set(i, rendered.get(match).setOptionChat(option.text == null ? "" : option.text)
                        .setOptionTooltip(option.tooltip == null ? "" : option.tooltip));
            } else replacements.get(i).resumeRendering();
        }
        ChatBoxUtil.chatBoxScreen.setChatOptions(replacements);
    }

    private static JsonObject optionBehavior(ChatBoxDialogues.Dialogues.Option option) {
        JsonObject value = ChatBoxUtil.GSON.toJsonTree(option).getAsJsonObject();
        value.remove("text");
        value.remove("tooltip");
        return value;
    }

    private static void refreshPortraits(ChatBoxDialogues previous, ChatBoxDialogues draft) {
        var old = portraitDefinitions(previous);
        var next = portraitDefinitions(draft);
        if (old.equals(next)) return;
        var rendered = ChatBoxUtil.chatBoxScreen.portraits;
        List<Portrait<?>> replacements = new ArrayList<>();
        boolean[] used = new boolean[old.size()];
        for (var definition : next) {
            int match = -1;
            if (old.size() == rendered.size()) for (int i = 0; i < old.size(); i++) {
                if (!used[i] && old.get(i).equals(definition)) { match = i; break; }
            }
            if (match >= 0) {
                used[match] = true;
                replacements.add(rendered.get(match));
            } else {
                var input = new ChatBoxDialogues.Dialogues();
                input.clearOldPortrait = false;
                input.portrait = List.of(definition);
                int start = replacements.size();
                input.setPortraitDialogues(replacements);
                for (int i = start; i < replacements.size(); i++) replacements.get(i).resumeRendering();
            }
        }
        ChatBoxUtil.chatBoxScreen.setPortrait(replacements);
    }

    private static List<JsonElement> portraitDefinitions(ChatBoxDialogues model) {
        var frames = model == null ? null : model.dialogues.get(ChatBoxUtil.group);
        List<JsonElement> definitions = new ArrayList<>();
        if (frames == null) return definitions;
        for (int i = 0; i <= ChatBoxUtil.index && i < frames.size(); i++) {
            var source = frames.get(i);
            if (Boolean.TRUE.equals(source.clearOldPortrait)) definitions.clear();
            else if (source.removePortrait != null) definitions.removeIf(p -> source.removePortrait.contains(portraitId(p)));
            if (source.portrait != null) for (var definition : source.portrait) {
                if (definition.isJsonObject()) {
                    var object = definition.getAsJsonObject();
                    if (object.has("replace") && object.get("replace").getAsBoolean())
                        definitions.removeIf(p -> Objects.equals(portraitId(p), portraitId(definition)));
                }
                definitions.add(definition);
            }
        }
        return definitions;
    }

    private static String portraitId(JsonElement definition) {
        if (definition.isJsonPrimitive()) return definition.getAsString();
        var id = definition.isJsonObject() ? definition.getAsJsonObject().get("id") : null;
        return id == null || id.isJsonNull() ? "" : id.getAsString();
    }

    private static ChatBoxDialogues.Dialogues frame(ChatBoxDialogues model, String group, Integer index) {
        var frames = model == null ? null : model.dialogues.get(group);
        return frames == null || index == null || index < 0 || index >= frames.size() ? null : frames.get(index);
    }

    private static ChatBoxDialogues parse(JsonObject request) {
        ChatBoxDialogues draft = ChatBoxUtil.GSON.fromJson(EditorFileStore.parse(request.get("content").getAsString()), ChatBoxDialogues.class);
        if (draft.dialogues == null || draft.dialogues.values().stream().anyMatch(frames -> frames == null
                || frames.stream().anyMatch(frame -> frame == null || frame.dialogBox == null)))
            throw new IllegalArgumentException("对话帧格式错误");
        return draft;
    }

    private static JsonObject operation(JsonObject request, String op) {
        JsonObject operation = request.deepCopy();
        operation.addProperty("op", op);
        return operation;
    }

    private static void requireSession(JsonObject request) {
        if (!isActive() || !session.equals(request.get("session").getAsString())
                || !resourceId.toString().equals(request.get("resourceId").getAsString())
                || request.get("version").getAsInt() <= version)
            throw new IllegalStateException("调试会话已变化，请重新跳转对话");
    }

    private static void install(JsonObject request, ChatBoxDialogues draft) {
        Minecraft mc = Minecraft.getInstance();
        session = request.get("session").getAsString();
        resourceId = ChatBox.parseId(request.get("resourceId").getAsString());
        version = request.get("version").getAsInt();
        sequence = 0;
        var id = resourceId;
        var oldScreen = ChatBoxUtil.chatBoxScreen;
        var oldTheme = ChatBoxUtil.chatBoxTheme;
        var oldHistory = ChatBoxUtil.historicalDialogue;
        var oldId = ChatBoxUtil.dialoguesResourceLocation;
        var oldGroup = ChatBoxUtil.group;
        var oldIndex = ChatBoxUtil.index;
        var oldDraft = ChatBoxUtil.dialoguesMap.get(id);
        var oldThemeId = ChatBoxUtil.themeResourceLocation;
        var animations = new HashMap<>(ChatBoxUtil.animationMap);
        boolean oldMode = ChatBoxUtil.isScreen, oldOpen = ChatBoxRender.isOpenChatBox, oldRender = ChatBoxRender.shouldRender;
        boolean oldPause = mc.options.pauseOnLostFocus;
        var previousScreen = mc.screen;
        boolean resumeBgm = SoundUtil.isSoundActive(oldScreen.bgm);
        restore = () -> {
            if (mc.screen == ChatBoxUtil.chatBoxScreen) mc.setScreen(null);
            var screen = ChatBoxUtil.chatBoxScreen;
            if (screen.video != null) screen.video.removeOnNext = true;
            screen.setVideo(null).playBgm("");
            SoundUtil.stopSound(screen.voice);
            ChatBoxUtil.chatBoxScreen = oldScreen;
            ChatBoxUtil.chatBoxTheme = oldTheme;
            ChatBoxUtil.historicalDialogue = oldHistory;
            ChatBoxUtil.dialoguesResourceLocation = oldId;
            ChatBoxUtil.group = oldGroup;
            ChatBoxUtil.index = oldIndex;
            ChatBoxUtil.themeResourceLocation = oldThemeId;
            ChatBoxUtil.isScreen = oldMode;
            ChatBoxRender.isOpenChatBox = oldOpen;
            ChatBoxRender.shouldRender = oldRender;
            mc.options.pauseOnLostFocus = oldPause;
            ChatBoxUtil.animationMap.clear(); ChatBoxUtil.animationMap.putAll(animations);
            if (oldDraft == null) ChatBoxUtil.dialoguesMap.remove(id); else ChatBoxUtil.dialoguesMap.put(id, oldDraft);
            if (resumeBgm && mc.level != null) oldScreen.playBgm(oldScreen.bgm);
            if (previousScreen instanceof ChatBoxScreen && mc.level != null) mc.setScreen(previousScreen);
        };
        try {
            SoundUtil.stopSound(oldScreen.bgm);
            ChatBoxUtil.chatBoxScreen = new DebugScreen();
            ChatBoxUtil.historicalDialogue = new HistoricalDialogueScreen();
            var sourceId = draft.theme == null ? null : ResourceLocation.tryParse(draft.theme);
            var sourceTheme = sourceId == null ? oldTheme : ChatBoxUtil.themeMap.get(sourceId);
            if (sourceTheme == null) throw new IllegalArgumentException("调试对话的主题不存在");
            ChatBoxUtil.chatBoxTheme = ChatBoxUtil.GSON.fromJson(ChatBoxUtil.GSON.toJsonTree(sourceTheme), ChatBoxTheme.class).setDefaultValue();
            var theme = ChatBoxUtil.chatBoxTheme;
            ChatBoxUtil.themeResourceLocation = draft.theme;
            ChatBoxUtil.animationMap.putAll(theme.customAnimation);
            ChatBoxUtil.chatBoxScreen.setDialogBox(theme.dialogBox.setDialogBoxTheme(ChatBoxUtil.chatBoxScreen.dialogBox))
                    .setFunctionalButtons(ChatBoxTheme.setButtonTheme(theme.functionalButton))
                    .setKeyPromptRender(theme.keyPrompt.setKeyPromptTheme(new com.zhenshiz.chatbox.component.KeyPromptRender()));
            ChatBoxUtil.dialoguesMap.put(id, draft);
            ChatBoxUtil.dialoguesResourceLocation = null;
            ChatBoxRender.shouldRender = false;
            ChatBoxUtil.isScreen = true;
            mc.options.pauseOnLostFocus = false;
            ChatBoxUtil.skipDialogues(id, request.get("group").getAsString(), request.get("index").getAsInt());
        } catch (RuntimeException error) { fail(error); throw error; }
    }

    /** 对话跳转成功后向网页推送当前组与句子的位置。 */
    public static void position() {
        if (!isActive()) return;
        JsonObject state = new JsonObject();
        state.addProperty("event", "debug.state");
        state.addProperty("session", session);
        state.addProperty("sequence", ++sequence);
        state.addProperty("version", version);
        state.addProperty("resourceId", String.valueOf(ChatBoxUtil.dialoguesResourceLocation));
        state.addProperty("group", ChatBoxUtil.group);
        state.addProperty("index", ChatBoxUtil.index);
        EditorBridgeClient.sendEvent(state);
    }

    /** 恢复调试前的客户端状态，并释放对应的服务端草稿。 */
    public static void stop() {
        generation++;
        pendingSession = "";
        if (!isActive()) return;
        Runnable action = restore;
        restore = null;
        String ended = session;
        try { action.run(); }
        finally {
            JsonObject operation = new JsonObject();
            operation.addProperty("op", "debug.stop");
            operation.addProperty("session", ended);
            operation.addProperty("serverSession", EditorBridgeClient.serverSession());
            EditorFileClient.request(operation).exceptionally(error -> null);
            JsonObject event = new JsonObject(); event.addProperty("event", "debug.closed"); event.addProperty("session", ended);
            EditorBridgeClient.sendEvent(event);
            session = ""; resourceId = null;
        }
    }

    static void stop(JsonObject request) {
        String expected = request.get("session").getAsString();
        if (expected.equals(session) || expected.equals(pendingSession)) stop();
    }

    private static void fail(RuntimeException error) {
        JsonObject event = new JsonObject(); event.addProperty("event", "debug.error"); event.addProperty("session", session);
        event.addProperty("error", error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        EditorBridgeClient.sendEvent(event);
        ChatBox.LOGGER.warn("Interactive dialogue debug failed", error);
        stop();
    }

    private static final class DebugScreen extends ChatBoxScreen {
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float tick) {
            try { super.render(graphics, x, y, tick); } catch (RuntimeException error) { fail(error); }
        }
        @Override public void tick() { try { super.tick(); } catch (RuntimeException error) { fail(error); } }
        @Override public boolean mouseClicked(double x, double y, int button) {
            try { return super.mouseClicked(x, y, button); } catch (RuntimeException error) { fail(error); return true; }
        }
        @Override public boolean keyPressed(int key, int scan, int modifiers) {
            try { return super.keyPressed(key, scan, modifiers); } catch (RuntimeException error) { fail(error); return true; }
        }
    }
}
