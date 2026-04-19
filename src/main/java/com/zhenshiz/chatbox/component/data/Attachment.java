package com.zhenshiz.chatbox.component.data;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;

@AllArgsConstructor
@NoArgsConstructor
public class Attachment {
    public String type = "texture";
    public String value = "";
    public Float x = 0f;
    public Float y = 0f;
    public Float width = 0f;
    public Float height = 0f;
    public String textAlign = "left";
    public Integer textColor = -1;
    public Boolean lineBreak = false;
    public Boolean parseText = true;
    public transient AbstractComponent<?> component;

    public Attachment setComponent(AbstractComponent<?> component) {
        this.component = component;
        return this;
    }

    public Attachment mapParameter() {
        return new Attachment(type, value,
                IPosition.calWidth(x), IPosition.calHeight(y),
                component.widthReference.getLength(width), component.heightReference.getLength(height),
                textAlign, textColor, lineBreak, parseText, component
        );
    }

    public static Attachment ofTexture(String texture, Float x, Float y, Float width, Float height) {
        return new Attachment("texture", texture, x, y, width, height, "left", -1, false, true, null);
    }

    public static Attachment ofText(String text, Float x, Float y, Float width, String textAlign, int color, boolean lineBreak, boolean parseText) {
        return new Attachment("text", text, x, y, width, 0f, textAlign, color, lineBreak, parseText, null);
    }

    public void render(GuiGraphics guiGraphics, float ox, float oy) {
        if (value.isEmpty()) return;
        var a = mapParameter();
        String type = this.type.toLowerCase();
        switch (type) {
            case "texture" -> RenderUtil.renderImageInner(guiGraphics, ChatBox.parseId(value), ox + a.x, oy + a.y, 1, 1, a.width, a.height);
            case "text" -> {
                String text = parseText ? ChatBoxUtil.parseText(RenderUtil.translated(value), lineBreak) : value;
                RenderUtil.drawStringAlign(guiGraphics, text, (int) (ox + a.x), (int) (oy + a.y), (int) (float) a.width, AbstractComponent.AlignX.of(textAlign), textColor, lineBreak);
            }
        }
    }
}
