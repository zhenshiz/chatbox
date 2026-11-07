package com.zhenshiz.chatbox.component;

import com.google.gson.JsonElement;
import com.zhenshiz.chatbox.component.data.Attachment;
import com.zhenshiz.chatbox.component.data.ChatBoxText;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import net.minecraft.client.gui.GuiGraphics;

public class DialogBox extends Portrait<DialogBox> {
    public static final String dialog_box = "chatbox:textures/chatbox/default_dialog_box.png";
    //对话框文本
    private ChatBoxText text = ChatBoxText.EMPTY_TEXT;
    //文本x位置
    public float textX = 0;
    //文本y位置
    public float textY = 0;
    //文本对齐方式
    public AlignX textAlign = AlignX.LEFT;
    //名称
    public ChatBoxText name = ChatBoxText.EMPTY_TEXT;
    //名称x位置
    public float nameX = 0;
    //名称y位置
    public float nameY = 0;
    //一行文本的宽度
    public float lineWidth = 100f;

    //全部文字是否全部显示
    public boolean isAllOver;

    @Override
    public String getId() {return "dialog_box";}

    @Override
    public String getTexture() {return BeanUtil.getValueOrDefault(super.getTexture(), dialog_box);}

    public DialogBox setText(JsonElement text) {
        this.text = ChatBoxText.of(text, true).setOnEnd(() -> setAllOver(true));
        return this;
    }
    public DialogBox setText(String text) {
        this.text = ChatBoxText.of(text, true).setOnEnd(() -> setAllOver(true));
        return this;
    }

    public DialogBox setTextAlign(String textAlign) {
        if (textAlign != null) this.textAlign = AlignX.of(textAlign);
        return this;
    }

    public DialogBox setName(JsonElement name) {
        this.name = ChatBoxText.of(name);
        return this;
    }
    public DialogBox setName(String name) {
        this.name = ChatBoxText.of(name);
        return this;
    }

    public DialogBox setNamePosition(float x, float y) {
        this.nameX = x;
        this.nameY = y;
        return this;
    }

    public DialogBox setTextPosition(float x, float y) {
        this.textX = x;
        this.textY = y;
        return this;
    }

    public DialogBox setLineWidth(Float lineWidth) {
        if (checkSize(lineWidth)) this.lineWidth = lineWidth;
        return this;
    }

    public DialogBox setAllOver(boolean allOver) {
        this.isAllOver = allOver;
        // 全部文字显示完成时触发ON_END事件
        if (allOver) {
            fireEvent("ON_END");
            text.charIndex = -114; // 不能直接调text.setAllOver(true)，否则会嵌套调用
        } else text.setAllOver(false);
        return this;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        renderInner(mouseX, mouseY);
        //chatBox image
        renderImage(guiGraphics, getRenderResource(), addTempAttachment(
                Attachment.ofText(this.name, this.nameX, this.nameY, this.lineWidth, this.textAlign.name(), -1, false),
                Attachment.ofText(this.text, this.textX, this.textY, this.lineWidth, this.textAlign.name(), -1, true)
        ));
    }
}
