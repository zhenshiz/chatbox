package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonObject;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.WeakHashMap;

/** Keeps unsaved dialogue drafts scoped to one authorized debugging player. */
public final class EditorDebugServer {
    private record Draft(String session, ResourceLocation id, int version, ChatBoxDialogues model, long expires) { }
    private static final Map<ServerPlayer, Draft> drafts = new WeakHashMap<>();

    private EditorDebugServer() { }

    static JsonObject execute(ServerPlayer player, JsonObject request) {
        String op = request.get("op").getAsString();
        if (!java.util.List.of("debug.start", "debug.update", "debug.stop").contains(op)) throw new IllegalArgumentException("Unknown debug operation");
        String session = request.get("session").getAsString();
        Draft previous = drafts.get(player);
        if (request.get("op").getAsString().equals("debug.stop")) {
            if (previous != null && previous.session.equals(session)) drafts.remove(player);
            return new JsonObject();
        }
        ResourceLocation id = ResourceLocation.parse(request.get("resourceId").getAsString());
        if (!ChatBoxDialoguesLoader.parsedDialogues.containsKey(id)) throw new IllegalArgumentException("该对话不在游戏已加载的文件清单中");
        int version = request.get("version").getAsInt();
        if (version < 1 || session.isBlank() || session.length() > 80) throw new IllegalArgumentException("Invalid debug session");
        if (request.get("op").getAsString().equals("debug.update")
                && (previous == null || !previous.session.equals(session) || !previous.id.equals(id) || version <= previous.version))
            throw new IllegalStateException("调试会话已变化，请重新跳转对话");
        ChatBoxDialogues model = ChatBoxDialoguesLoader.GSON.fromJson(EditorFileStore.parse(request.get("content").getAsString()), ChatBoxDialogues.class);
        if (model.dialogues == null || model.dialogues.values().stream().anyMatch(frames -> frames == null
                || frames.stream().anyMatch(frame -> frame == null || frame.dialogBox == null)))
            throw new IllegalArgumentException("对话帧格式错误");
        drafts.put(player, new Draft(session, id, version, model, System.nanoTime() + 15_000_000_000L));
        JsonObject result = new JsonObject();
        result.addProperty("version", version);
        return result;
    }

    static void touch(ServerPlayer player) {
        Draft draft = drafts.get(player);
        if (draft != null) drafts.put(player, new Draft(draft.session, draft.id, draft.version, draft.model, System.nanoTime() + 15_000_000_000L));
    }

    /**
     * Reports whether a player owns a temporary draft, including an expired draft awaiting cleanup.
     * @param player the server-side player
     * @return whether ordinary party dialogue propagation must be suppressed
     */
    public static boolean isDebugging(ServerPlayer player) { return drafts.containsKey(player); }

    /**
     * Rechecks permission and the connection lease before processing dialogue actions.
     * @param player the sender of a dialogue action
     * @return whether the action can execute in the current session
     */
    public static boolean permitsAction(ServerPlayer player) {
        Draft draft = drafts.get(player);
        if (draft == null) return true;
        if (System.nanoTime() >= draft.expires) { drafts.remove(player); return false; }
        return player.createCommandSourceStack().hasPermission(4);
    }

    /**
     * Resolves a player's draft without changing the dialogue data used by other players.
     * @param player the player entering a dialogue
     * @param id the dialogue file resource ID
     * @return the permitted draft or loaded dialogue, or {@code null} if access has expired
     */
    public static ChatBoxDialogues dialogue(ServerPlayer player, ResourceLocation id) {
        if (!permitsAction(player)) return null;
        Draft draft = drafts.get(player);
        return draft != null && draft.id.equals(id) ? draft.model : ChatBoxDialoguesLoader.parsedDialogues.get(id);
    }
}
