package com.zhenshiz.chatbox.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;

public class DialogBox extends AbstractComponent<DialogBox> {
    public ResourceLocation texture;
    public Component text;
    public Component name;
    public int nameX;
    public int nameY;
    public int textX;
    public int textY;
    public int lineWidth;
    //全部文字是否全部显示
    public boolean isAllOver;

    public int tickCount;
    private String[] textBuffer;

    public DialogBox() {
        this.texture = ChatBox.ResourceLocationMod("textures/chatbox/default_dialog_box.png");
        this.text = CommonComponents.EMPTY;
        this.name = CommonComponents.EMPTY;
        this.isAllOver = false;
        this.lineWidth = 100;
        this.tickCount = 0;
        this.textBuffer = new String[]{""};
        defaultOption();
    }

    public DialogBox setTexture(ResourceLocation texture) {
        if (texture != null) this.texture = texture;
        return this;
    }

    public DialogBox setTexture(String texture) {
        if (texture != null) return setTexture(ResourceLocation.parse(texture));
        return this;
    }

    public DialogBox setText(String text,boolean isTranslatable) {
        if (text != null) {
            this.text = isTranslatable ? Component.translatable(text): Component.nullToEmpty(text);
            textToTextBuffer();
        }
        return this;
    }

    public DialogBox setName(String name,boolean isTranslatable) {
        if (name != null) this.name = isTranslatable ? Component.translatable(name): Component.nullToEmpty(name);
        return this;
    }

    public DialogBox setNamePosition(int x, int y) {
        if (isResponsiveSkew(x) && isResponsiveSkew(y)) {
            this.nameX = x;
            this.nameY = y;
        }
        return this;
    }

    public DialogBox setNamePosition(Integer[] namePosition) {
        if (namePosition != null) return setNamePosition(namePosition[0], namePosition[1]);
        return this;
    }

    public DialogBox setTextPosition(int x, int y) {
        if (isResponsiveSkew(x) && isResponsiveSkew(y)) {
            this.textX = x;
            this.textY = y;
        }
        return this;
    }

    public DialogBox setTextPosition(Integer[] textPosition) {
        if (textPosition != null) return setTextPosition(textPosition[0], textPosition[1]);
        return this;
    }

    public DialogBox setLineWidth(int lineWidth) {
        if (isResponsiveSize(lineWidth)) this.lineWidth = lineWidth;
        return this;
    }

    public DialogBox setAllOver(boolean allOver) {
        this.isAllOver = allOver;
        return this;
    }

    public DialogBox resetTickCount() {
        this.tickCount = 0;
        return this;
    }

    private void textToTextBuffer() {
        String input = this.text.getString();
        List<String> result = new ArrayList<>();
        int index = 0;
        StringBuilder stringBuilder = new StringBuilder();

        while (index < input.length()) {
            char c = input.charAt(index);
            stringBuilder.append(c);
            if (c == '\\' || c == '§') {
                stringBuilder.append(input.charAt(index + 1));
                index++;
            }
            result.add(stringBuilder.toString());
            index++;
        }

        this.textBuffer = result.toArray(new String[0]);
    }

    public void click(int pMouseX, int pMouseY, boolean isOptionExist) {
        if (!this.isAllOver) {
            //未全部加载时，点击显示所有文本
            this.tickCount = this.textBuffer.length - 1;
            setAllOver(true);
        } else {
            //全部点击时触发
            if (!isOptionExist && minecraft.player != null) {
                //只有没有选项的时候才能通过点击空白处跳转到下一句话
                setIndex(this.index + 1);
                ChatBoxUtil.skipDialogues(minecraft.player.getUUID(), this.dialoguesResourceLocation, this.group, this.index);
            }
        }
    }

    public void tick() {
        if (!this.isAllOver) {
            //未全部加载，开始加载
            this.tickCount++;
            if (this.tickCount == this.textBuffer.length - 1) setAllOver(true);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        //chatBox image
        renderImage(guiGraphics, this.texture);

        //name and text
        Vec2 position = getCurrentPosition();
        int x = (int) position.x;
        int y = (int) position.y;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        if (StrUtil.isNotEmpty(this.name.getString())) {
            guiGraphics.drawWordWrap(minecraft.font, Component.nullToEmpty(parseText(StrUtil.format("[{}]", name.getString()))), getResponsiveWidth(x + this.nameX), getResponsiveHeight(y + this.nameY), getResponsiveWidth(this.lineWidth), CommonColors.WHITE);
        }
        if (StrUtil.isNotEmpty(this.text.getString())) {
            guiGraphics.drawWordWrap(minecraft.font, Component.nullToEmpty(parseText(this.textBuffer[this.tickCount])), getResponsiveWidth(x + this.textX), getResponsiveHeight(y + this.textY), getResponsiveWidth(this.lineWidth), CommonColors.WHITE);
        }
        poseStack.popPose();
    }
}
