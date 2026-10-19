package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditorPreviewThemeTest {
    @Test void dialogueTestNeedsNoLoadedThemeAndKeepsKeyPromptsVisible() {
        JsonObject theme = EditorPreviewTheme.dialogue(null);
        assertTrue(theme.getAsJsonObject("keyPrompt").get("visible").getAsBoolean());
        assertFalse(theme.getAsJsonObject("dialogBox").has("hidden"));
        assertFalse(theme.getAsJsonObject("option").has("hidden"));
        assertEquals(3, theme.getAsJsonArray("functionalButton").size());
        JsonObject option = theme.getAsJsonObject("option");
        float slotHeight = option.get("height").getAsFloat();
        float renderedHeight = slotHeight * option.get("scale").getAsFloat();
        assertTrue(renderedHeight > 0 && renderedHeight < slotHeight, "Adjacent test options have a visible gap");
    }

    @Test void referencedThemeOnlySuppliesPortraitResourcesAndNeverItsConversationUi() {
        JsonObject source = JsonParser.parseString("""
                {
                  "dialogBox":{"texture":"other:box.png","hidden":true},
                  "option":{"hidden":true,"height":100},
                  "keyPrompt":{"visible":false,"x":-1000},
                  "functionalButton":[],
                  "portrait":{"actor":{"texture":"test:actor.png"}},
                  "customAnimation":{"intro":[{"time":10,"opacity":100}]}
                }
                """).getAsJsonObject();
        JsonObject before = source.deepCopy();
        JsonObject actual = EditorPreviewTheme.dialogue(source);
        JsonObject defaults = EditorPreviewTheme.dialogue(null);
        for (String field : new String[]{"dialogBox", "option", "keyPrompt", "functionalButton"}) {
            assertEquals(defaults.get(field), actual.get(field), field);
        }
        assertEquals(source.get("portrait"), actual.get("portrait"));
        assertEquals(source.get("customAnimation"), actual.get("customAnimation"));
        actual.getAsJsonObject("portrait").getAsJsonObject("actor").addProperty("texture", "test:changed.png");
        actual.getAsJsonObject("customAnimation").getAsJsonArray("intro").get(0).getAsJsonObject().addProperty("opacity", 0);
        assertEquals(before, source, "Testing never modifies the loaded theme");
    }
}
