package com.zhenshiz.chatbox.editor;

import com.google.gson.*;
import java.util.Locale;
import java.util.Set;

/** 清除可执行指令与脚本，保留对话的视觉效果和跳转事件。 */
public final class EditorPreviewSanitizer {
    private static final Set<String> VISUAL_EVENTS = Set.of("JUMP", "GOTO_NEXT", "PLAY_VOICE", "PLAY_SOUND", "STOP_SOUND", "SHOW", "HIDE", "REPLACE", "LOCK", "UNLOCK", "SET_NORMAL", "SET_AUTOPLAY", "SCALE", "RESTART_ANIMATION", "PLAY_ANIMATION");
    private EditorPreviewSanitizer() { }

    /**
     * 判断事件是否能在不执行项目脚本或指令的情况下运行。
     *
     * @param type 不区分大小写的事件类型
     * @return 是否为支持的视觉效果或跳转事件
     */
    public static boolean isVisualEvent(String type) {
        return type != null && VISUAL_EVENTS.contains(type.toUpperCase(Locale.ROOT));
    }

    private static boolean isSafeEvent(JsonElement event) {
        if (!event.isJsonObject()) return false;
        JsonObject object = event.getAsJsonObject();
        JsonElement type = object.get("type");
        JsonElement condition = object.get("condition");
        return type != null && type.isJsonPrimitive() && isVisualEvent(type.getAsString())
                && (condition == null || condition.isJsonNull()
                || condition.isJsonPrimitive() && condition.getAsString().isBlank());
    }

    static void sanitize(JsonElement element) {
        if (element.isJsonArray()) { element.getAsJsonArray().forEach(EditorPreviewSanitizer::sanitize); return; }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        object.remove("command");
        object.remove("unlockCommand");
        object.remove("criteria");
        object.remove("video");
        if (object.has("renderEvents") && object.get("renderEvents").isJsonArray()) {
            object.getAsJsonArray("renderEvents").asList().removeIf(event -> !isSafeEvent(event));
        }
        if (object.has("click") && object.get("click").isJsonObject()) {
            var click = object.getAsJsonObject("click");
            if (!isSafeEvent(click)) {
                click.remove("condition");
                click.addProperty("type", "JUMP");
                click.addProperty("value", "this");
            }
        }
        object.remove("condition");
        object.entrySet().forEach(entry -> sanitize(entry.getValue()));
    }

}
