package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** 提供固定的对话测试布局，避免继承已加载对话的界面状态。 */
final class EditorPreviewTheme {
    private EditorPreviewTheme() { }

    /**
     * 创建对话测试主题，可从源主题继承立绘和动画定义。
     *
     * <p>源主题的对话框、选项、按钮和按键提示不影响此布局。
     * 修改返回的主题不会影响源主题。
     *
     * @param source 提供立绘资源的 JSON 主题，可以为 {@code null}
     * @return 带有可见按键提示和独立选项行的新主题
     */
    static JsonObject dialogue(JsonObject source) {
        JsonObject theme = JsonParser.parseString("""
                {
                  "dialogBox": {
                    "texture":"chatbox:textures/chatbox/default_dialog_box.png",
                    "alignY":"bottom", "width":100, "height":40, "lineWidth":70,
                    "nameX":20, "nameY":10, "textX":20, "textY":15
                  },
                  "option": {
                    "texture":"chatbox:textures/options/default_no_checked_option.png",
                    "hoverTexture":"chatbox:textures/options/default_checked_option.png",
                    "lockTexture":"chatbox:textures/options/default_lock_checked_option.png",
                    "x":0, "y":12, "width":50, "height":8, "scale":0.9,
                    "alignX":"center", "alignY":"top",
                    "textAlign":"center", "optionChatX":0, "optionChatY":2.7
                  },
                  "keyPrompt":{"visible":true, "x":1, "y":1},
                  "functionalButton":[{"type":"log"},{"type":"fastForward"},{"type":"autoPlay"}]
                }
                """).getAsJsonObject();
        if (source != null) {
            for (String field : new String[]{"portrait", "customAnimation"}) {
                if (source.has(field)) theme.add(field, source.get(field).deepCopy());
            }
        }
        return theme;
    }
}
