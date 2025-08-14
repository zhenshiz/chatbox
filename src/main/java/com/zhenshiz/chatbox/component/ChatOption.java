package com.zhenshiz.chatbox.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.payload.c2s.SendCommandPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix3x2fStack;

import java.util.List;

public class ChatOption extends AbstractComponent<ChatOption> {
    //默认材质
    public ResourceLocation texture;
    //鼠标悬浮材质
    public ResourceLocation selectTexture;
    //上锁材质
    public ResourceLocation lockTexture;
    //选项文本
    public Component optionChat;
    //选项x位置
    public float optionChatX;
    //选项y位置
    public float optionChatY;
    //点击后触发内容
    public Runnable onClickEvent;
    //是否上锁
    public boolean isLock;
    //悬浮字体
    public Component optionTooltip;
    //文本对齐
    public TextAlign textAlign;
    //选项连接的下一个对话
    public String next;
    //是否选择，用于render对话框
    public boolean isSelect;

    public ChatOption() {
        setTextures(ChatBox.ResourceLocationMod("textures/options/default_no_checked_option.png"));
        setSelectTexture(ChatBox.ResourceLocationMod("textures/options/default_checked_option.png"));
        setLockTexture(ChatBox.ResourceLocationMod("textures/options/default_no_checked_option.png"));
        setOptionChat("", false);
        setOptionChatPosition(0, 0);
        setClickEvent(() -> {});
        setIsLock(false);
        setOptionTooltip("", false);
        setTextAlign(TextAlign.LEFT);
        setNext("");
        defaultOption();
        setIsSelect(false);
    }

    public ChatOption setOptionChat(String optionChat, boolean isTranslatable) {
        if (optionChat != null)
            this.optionChat = isTranslatable ? Component.translatable(optionChat) : Component.nullToEmpty(optionChat);
        return this;
    }

    public ChatOption setOptionTooltip(String optionTooltip, boolean isTranslatable) {
        if (optionTooltip != null)
            this.optionTooltip = isTranslatable ? Component.translatable(optionTooltip) : Component.nullToEmpty(optionTooltip);
        return this;
    }

    public ChatOption setTextures(ResourceLocation textures) {
        if (textures != null) this.texture = textures;
        return this;
    }

    public ChatOption setTextures(String textures) {
        if (textures != null) return setTextures(ResourceLocation.tryParse(textures));
        return this;
    }

    public ChatOption setSelectTexture(ResourceLocation selectTexture) {
        if (selectTexture != null) this.selectTexture = selectTexture;
        return this;
    }

    public ChatOption setSelectTexture(String selectTexture) {
        if (selectTexture != null) return setSelectTexture(ResourceLocation.tryParse(selectTexture));
        return this;
    }

    public ChatOption setLockTexture(ResourceLocation lockTexture) {
        if (lockTexture != null) this.lockTexture = lockTexture;
        return this;
    }

    public ChatOption setLockTexture(String lockTexture) {
        if (lockTexture != null) return setLockTexture(ResourceLocation.tryParse(lockTexture));
        return this;
    }

    public ChatOption setClickEvent(Runnable onClickEvent) {
        if (onClickEvent != null) this.onClickEvent = onClickEvent;
        return this;
    }

    public ChatOption setClickEvent(String type, String value) {
        if (type != null && value != null) {
            this.onClickEvent = () -> {
                if (minecraft.player != null) {
                    if (type.equals("command")) {
                        var commands = value.split(";");
                        for (var command : commands) {
                            command = command.trim();
                            if (!command.isBlank()) ClientPlayNetworking.send(new SendCommandPayload(command));
                        }
                    }
                }
            };
        }
        return this;
    }

    public ChatOption setIsLock(boolean isLock) {
        this.isLock = isLock;
        return this;
    }

    public ChatOption setNext(String next) {
        if (next != null) this.next = next;
        return this;
    }

    public ChatOption setOptionChatPosition(float x, float y) {
        this.optionChatX = x;
        this.optionChatY = y;
        return this;
    }

    public ChatOption setTextAlign(TextAlign textAlign) {
        if (textAlign != null) this.textAlign = textAlign;
        return this;
    }

    public ChatOption setIsSelect(boolean isSelect) {
        this.isSelect = isSelect;
        return this;
    }

    public void click() {
        if (!this.isLock && minecraft.player != null) {
            //触发自定义事件
            this.onClickEvent.run();
            //跳转到指定的对话或者其它模块的对话
            if (StrUtil.isEmpty(this.next)) {
                //跳转下一句话
                ChatBoxUtil.skipDialogues(this.dialoguesResourceLocation, this.group, this.index + 1);
            } else if (StrUtil.isInteger(this.next)) {
                //如果为数字跳转到指定序号的对话
                int index = Integer.parseInt(this.next);
                ChatBoxUtil.skipDialogues(this.dialoguesResourceLocation, this.group, index);
            } else {
                //如果是英文则跳转到指定模块的对话
                ChatBoxUtil.skipDialogues(this.dialoguesResourceLocation, this.next);
            }

        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        Vec2 pos = getCurrentPosition();
        float x = pos.x;
        float y = pos.y;
        int color = CommonColors.WHITE;
        ResourceLocation texture = this.texture;
        if (this.isLock) {
            texture = this.lockTexture;
            color = CommonColors.GRAY;
        } else if (isSelect(mouseX, mouseY)) {
            texture = this.selectTexture;
            color = CommonColors.YELLOW;
        }

        renderInCommon(guiGraphics, texture, color, x, y);

        //render tooltip
        if (!this.optionTooltip.getString().isEmpty() && isSelect(mouseX, mouseY)) {
            guiGraphics.renderTooltip(minecraft.font, List.of(new ClientTextTooltip(optionTooltip.getVisualOrderText())), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, float pPartialTick) {
        Vec2 pos = getCurrentPosition();
        float x = pos.x;
        float y = pos.y;
        int color = CommonColors.WHITE;
        ResourceLocation texture = this.texture;
        if (this.isLock) {
            texture = this.lockTexture;
            color = CommonColors.GRAY;
        } else if (isSelect) {
            texture = this.selectTexture;
            color = CommonColors.YELLOW;
        }

        renderInCommon(guiGraphics, texture, color, x, y);
    }

    private void renderInCommon(GuiGraphics guiGraphics, ResourceLocation texture, int color, float x, float y) {
        //render image
        if (texture != null) renderImage(guiGraphics, texture);

        //render option text
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        switch (this.textAlign) {
            case LEFT ->
                    RenderUtil.drawLeftScaleText(guiGraphics, Component.nullToEmpty(parseText(optionChat.getString())), (int) getResponsiveWidth(x + this.width / 2 + this.optionChatX), (int) getResponsiveHeight(y + this.height / 2 + this.optionChatY), 1, false, color);
            case CENTER ->
                    RenderUtil.drawCenterScaleText(guiGraphics, Component.nullToEmpty(parseText(optionChat.getString())), (int) getResponsiveWidth(x + this.width / 2 + this.optionChatX), (int) getResponsiveHeight(y + this.height / 2 + this.optionChatY), 1, false, color);
            case RIGHT ->
                    RenderUtil.drawRightScaleText(guiGraphics, Component.nullToEmpty(parseText(optionChat.getString())), (int) getResponsiveWidth(x + this.width / 2 + this.optionChatX), (int) getResponsiveHeight(y + this.height / 2 + this.optionChatY), 1, false, color);
        }
        poseStack.popMatrix();
    }

    public enum TextAlign {
        LEFT,
        CENTER,
        RIGHT;

        public static TextAlign of(String text) {
            if (text == null) return TextAlign.LEFT;
            return valueOf(text.toUpperCase());
        }
    }
}
