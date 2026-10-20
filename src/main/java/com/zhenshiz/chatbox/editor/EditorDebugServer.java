package com.zhenshiz.chatbox.editor;

import com.zhenshiz.chatbox.ChatBox;
import com.google.gson.JsonObject;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.WeakHashMap;

/** 将未保存的对话草稿限定在一个已授权的调试玩家会话内。 */
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
        ResourceLocation id = ChatBox.parseId(request.get("resourceId").getAsString());
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
     * 判断玩家是否持有临时草稿，包括等待清理的过期草稿。
     * @param player 服务端玩家
     * @return 是否应禁止向队伍传播普通对话
     */
    public static boolean isDebugging(ServerPlayer player) { return drafts.containsKey(player); }

    /**
     * 处理对话操作前重新校验权限与连接有效期。
     * @param player 发起对话操作的玩家
     * @return 是否允许在当前会话执行操作
     */
    public static boolean permitsAction(ServerPlayer player) {
        Draft draft = drafts.get(player);
        if (draft == null) return true;
        if (System.nanoTime() >= draft.expires) { drafts.remove(player); return false; }
        return player.createCommandSourceStack().hasPermission(4);
    }

    /**
     * 获取玩家的草稿，不改变其他玩家使用的对话数据。
     * @param player 正在进入对话的玩家
     * @param id 对话文件资源 ID
     * @return 可访问的草稿或已加载对话；访问已过期时返回 {@code null}
     */
    public static ChatBoxDialogues dialogue(ServerPlayer player, ResourceLocation id) {
        if (!permitsAction(player)) return null;
        Draft draft = drafts.get(player);
        return draft != null && draft.id.equals(id) ? draft.model : ChatBoxDialoguesLoader.parsedDialogues.get(id);
    }
}
