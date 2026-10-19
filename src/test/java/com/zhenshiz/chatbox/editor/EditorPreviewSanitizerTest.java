package com.zhenshiz.chatbox.editor;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorPreviewSanitizerTest {
    @Test void removesCommandsVideosCriteriaAndCustomActionsFromNestedDialogue() {
        JsonObject original = JsonParser.parseString("""
                {"dialogues":{"start":[{"command":"say hidden", "video":{"path":"remote.mp4"},
                "dialogBox":{"text":"demo:welcome"},"renderEvents":[{"type":"COMMAND","value":"say hidden"},{"type":"KUBEJS","value":"script"},{"type":"PLAY_SOUND","value":"minecraft:ui.button.click"}],
                "options":[{"text":"Continue","criteria":["unsafe"],"unlockCommand":"execute dangerous","click":{"type":"COMMAND","value":"op @s"}}]}]}}
                """).getAsJsonObject();
        JsonObject copy = original.deepCopy();
        EditorPreviewSanitizer.sanitize(copy);
        JsonObject frame = copy.getAsJsonObject("dialogues").getAsJsonArray("start").get(0).getAsJsonObject();
        assertFalse(frame.has("command"));
        assertFalse(frame.has("video"));
        assertEquals("demo:welcome", frame.getAsJsonObject("dialogBox").get("text").getAsString());
        assertEquals(1, frame.getAsJsonArray("renderEvents").size());
        assertEquals("PLAY_SOUND", frame.getAsJsonArray("renderEvents").get(0).getAsJsonObject().get("type").getAsString());
        JsonObject option = frame.getAsJsonArray("options").get(0).getAsJsonObject();
        assertFalse(option.has("criteria"));
        assertFalse(option.has("unlockCommand"));
        assertEquals("this", option.getAsJsonObject("click").get("value").getAsString());
        assertTrue(original.toString().contains("op @s"), "The saved project must remain unchanged");
    }

    @Test void retainsVisualEventsAndNavigationInThemes() {
        JsonObject theme = JsonParser.parseString("""
                {"portrait":{"actor":{"texture":"demo:textures/actor.png", "renderEvents":[{"type":"scale","value":"1.1"},{"type":"PLAY_ANIMATION","value":"intro"}]}},
                "option":{"click":{"type":"JUMP","value":"next"}},"dialogBox":{"renderEvents":[{"type":"SCRIPT","value":"hidden"}]}}
                """).getAsJsonObject();
        EditorPreviewSanitizer.sanitize(theme);
        assertEquals(2, theme.getAsJsonObject("portrait").getAsJsonObject("actor").getAsJsonArray("renderEvents").size());
        assertEquals("next", theme.getAsJsonObject("option").getAsJsonObject("click").get("value").getAsString());
        assertTrue(theme.getAsJsonObject("dialogBox").getAsJsonArray("renderEvents").isEmpty());
    }

    @Test void excludesMvelAndConditionalActionsWithoutExecutingTheirVisualFallback() {
        JsonObject content = JsonParser.parseString("""
                {"renderEvents":[
                  {"type":"MVEL","value":"player.kill()"},
                  {"type":"SHOW","condition":"player.kill(); true","value":"@s"},
                  {"type":"HIDE","condition":"server:true","value":"@s"},
                  {"type":"LOCK","condition":"execute as @s run say test","value":"@s"},
                  {"type":"SHOW","condition":null,"value":"@s"}],
                 "click":{"type":"JUMP","condition":"player.kill(); true","value":"other"}}
                """).getAsJsonObject();
        EditorPreviewSanitizer.sanitize(content);
        assertEquals(1, content.getAsJsonArray("renderEvents").size());
        assertFalse(content.getAsJsonArray("renderEvents").get(0).getAsJsonObject().has("condition"));
        assertEquals("this", content.getAsJsonObject("click").get("value").getAsString());
        assertFalse(content.toString().contains("player.kill"));
        assertFalse(EditorPreviewSanitizer.isVisualEvent("MVEL"));
        assertFalse(EditorPreviewSanitizer.isVisualEvent(null));
    }

    @Test void retainsNewComponentStateEvents() {
        JsonObject content = JsonParser.parseString("""
                {"renderEvents":[{"type":"LOCK","value":"option"},
                {"type":"unlock","value":"option"},{"type":"SET_NORMAL","value":"option"}]}
                """).getAsJsonObject();
        EditorPreviewSanitizer.sanitize(content);
        assertEquals(3, content.getAsJsonArray("renderEvents").size());
    }
}
