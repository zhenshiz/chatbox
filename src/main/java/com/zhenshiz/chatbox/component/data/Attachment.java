package com.zhenshiz.chatbox.component.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;

@AllArgsConstructor
@NoArgsConstructor
public class Attachment {
    public String type = "texture";
    public JsonElement value = ChatBoxText.EMPTY;
    public Float x = 0f;
    public Float y = 0f;
    public Float width = 0f;
    public Float height = 0f;
    public String textAlign = "left";
    public Integer textColor = -1;
    public Boolean lineBreak = false;
    public transient AbstractComponent<?> component;
    public transient ChatBoxText chatBoxText;

    public Attachment setComponent(AbstractComponent<?> component) {
        this.component = component;
        return this;
    }

    public Attachment mapParameter() {
        return new Attachment(type, value,
                IPosition.calWidth(x), IPosition.calHeight(y),
                component.widthReference.getLength(width), component.heightReference.getLength(height),
                textAlign, textColor, lineBreak, component, chatBoxText
        );
    }

    public static Attachment ofTexture(String texture, Float x, Float y, Float width, Float height) {
        return new Attachment("texture", new JsonPrimitive(texture), x, y, width, height, "left", -1, false, null, null);
    }

    public static Attachment ofText(ChatBoxText text, Float x, Float y, Float width, String textAlign, int color, boolean lineBreak) {
        return new Attachment("text", null, x, y, width, 0f, textAlign, color, lineBreak, null, text);
    }

    public void render(GuiGraphics guiGraphics, float ox, float oy) {
        var a = mapParameter();
        String type = this.type.toLowerCase();
        switch (type) {
            case "texture" -> {
                var value = a.value.getAsString();
                if (value.isEmpty()) return;
                RenderUtil.renderImageInner(guiGraphics, ChatBox.parseId(value), ox + a.x, oy + a.y, 1, 1, a.width, a.height);
            }
            case "text" -> {
                if (chatBoxText == null) chatBoxText = ChatBoxText.of(value);
                chatBoxText.drawText(guiGraphics, (int) (ox + a.x), (int) (oy + a.y), (int) (float) a.width, AbstractComponent.AlignX.of(textAlign), textColor, lineBreak);
            }
        }
    }
}
