package com.zhenshiz.chatbox.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.Config;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.*;

public class DialogBox extends AbstractComponent<DialogBox> {
    //默认材质
    public ResourceLocation texture;
    //对话框文本
    public String text;
    //文本x位置
    public float textX;
    //文本y位置
    public float textY;
    //名称
    public Component name;
    //名称x位置
    public float nameX;
    //名称y位置
    public float nameY;
    //一行文本的宽度
    public float lineWidth;

    //全部文字是否全部显示
    public boolean isAllOver;
    public int tickCount;
    private String[] textBuffer;
    private int charIndex;

    public DialogBox() {
        setTexture(ChatBox.ResourceLocationMod("textures/chatbox/default_dialog_box.png"));
        setText("", false);
        setTextPosition(0, 0);
        setName("", false);
        setNamePosition(0, 0);
        setLineWidth(100f);

        setAllOver(false);
        resetTickCount();
        this.textBuffer = new String[]{""};
    }

    public DialogBox setTexture(ResourceLocation texture) {
        if (texture != null) this.texture = texture;
        return this;
    }

    public DialogBox setTexture(String texture) {
        if (texture != null) return setTexture(ResourceLocation.tryParse(texture));
        return this;
    }

    public DialogBox setText(String text, boolean isTranslatable) {
        if (text != null) {
            this.text = isTranslatable ? Language.getInstance().getOrDefault(text) : text;
            if (ChatBox.isTextAnimatorLoaded()) this.text = this.text.replaceAll("<typewriter>", "");
            textToTextBuffer();
        }
        return this;
    }

    public DialogBox setName(String name, boolean isTranslatable) {
        if (name != null) this.name = isTranslatable ? Component.translatable(name) : Component.nullToEmpty(name);
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
        return this;
    }

    public DialogBox resetTickCount() {
        this.tickCount = 0;
        this.charIndex = 0;
        return this;
    }

    private void textToTextBuffer() {
        String input = this.text;
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

            if (ChatBox.isTextAnimatorLoaded() && c == '<') {
                int closing = input.indexOf('>', index + 1);
                if (closing != -1) {
                    stringBuilder.append(input, index + 1, closing + 1);
                    index = closing;
                }
            }

            result.add(stringBuilder.toString());
            index++;
        }

        this.textBuffer = result.toArray(new String[0]);
    }

    public void click(boolean gotoNext) {
        if (!this.isAllOver) {
            //未全部加载时，点击显示所有文本
            this.charIndex = this.textBuffer.length - 1;
            setAllOver(true);
        } else {
            //全部加载时触发
            if (gotoNext && minecraft.player != null) {
                //只有没有选项的时候才能通过点击空白处跳转到下一句话
                skipDialogues(dialoguesResourceLocation, group, index + 1);
                chatBoxScreen.tickAutoPlay = 20;
            }
        }
    }

    public void tick() {
        if (!this.isAllOver) {
            //未全部加载，开始加载
            if (this.charIndex == this.textBuffer.length - 1) {
                setAllOver(true);
                return;
            }
            this.tickCount++;
            float charPerTick = Config.charPerSecond.get() / 20f;
            if (tickCount * charPerTick >= charIndex + 1) {
                charIndex = Math.min((int) (tickCount * charPerTick), this.textBuffer.length - 1);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        //chatBox image
        if (this.texture != null) renderImage(guiGraphics, this.texture);

        //name and text
        Vec2 position = getCurrentPosition();
        float x = position.x;
        float y = position.y;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        if (StrUtil.isNotEmpty(this.name.getString())) {
            guiGraphics.drawWordWrap(minecraft.font, Component.nullToEmpty(parseText(StrUtil.format("[{}]", name.getString()))), (int) getResponsiveWidth(x + this.nameX), (int) getResponsiveHeight(y + this.nameY), (int) getResponsiveWidth(this.lineWidth), CommonColors.WHITE);
        }
        if (StrUtil.isNotEmpty(this.text)) {
            guiGraphics.drawWordWrap(minecraft.font, Component.nullToEmpty(parseText(this.textBuffer[this.charIndex])), (int) getResponsiveWidth(x + this.textX), (int) getResponsiveHeight(y + this.textY), (int) getResponsiveWidth(this.lineWidth), CommonColors.WHITE);
        }
        poseStack.popPose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, float pPartialTick) {
        render(guiGraphics, 0, 0, pPartialTick);
    }
}
