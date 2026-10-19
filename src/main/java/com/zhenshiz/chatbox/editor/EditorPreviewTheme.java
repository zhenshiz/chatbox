package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Supplies the fixed dialogue-test layout without inheriting a loaded conversation UI. */
final class EditorPreviewTheme {
    private EditorPreviewTheme() { }

    /**
     * Creates a dialogue-test theme with optional portrait and animation definitions.
     *
     * <p>The source's dialog box, options, buttons and key prompts do not affect this layout.
     * Changes to the result do not change the source theme.
     *
     * @param source the JSON theme providing portrait resources, or {@code null}
     * @return a new theme with visible key prompts and separated option rows
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
