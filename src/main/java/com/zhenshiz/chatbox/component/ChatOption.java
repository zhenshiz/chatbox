package com.zhenshiz.chatbox.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import net.minecraft.world.phys.Vec2;

public class ChatOption extends AbstractComponent<ChatOption> {
    public ResourceLocation texture;
    public ResourceLocation selectTexture;
    public ResourceLocation lockTexture;
    public Component optionChat;
    public int optionChatX;
    public int optionChatY;
    public Runnable onClickEvent;
    public boolean isLock;
    public Component optionTooltip;
    public TextAlign textAlign;
    public String next;

    public ChatOption() {
        this.texture = ChatBox.ResourceLocationMod("textures/options/default_no_checked_option.png");
        this.selectTexture = ChatBox.ResourceLocationMod("textures/options/default_checked_option.png");
        this.lockTexture = ChatBox.ResourceLocationMod("textures/options/default_no_checked_option.png");
        this.optionChat = CommonComponents.EMPTY;
        this.onClickEvent = () -> {
        };
        this.optionTooltip = CommonComponents.EMPTY;
        this.isLock = false;
        this.optionChatX = 0;
        this.optionChatY = 0;
        this.textAlign = TextAlign.LEFT;
        this.next = "";
        defaultOption();
    }

    public ChatOption setOptionChat(String optionChat,boolean isTranslatable) {
        if (optionChat != null) this.optionChat = isTranslatable ? Component.translatable(optionChat) : Component.nullToEmpty(optionChat);
        return this;
    }

    public ChatOption setOptionTooltip(String optionTooltip,boolean isTranslatable) {
        if (optionTooltip != null) this.optionTooltip = isTranslatable ? Component.translatable(optionTooltip) : Component.nullToEmpty(optionTooltip);
        return this;
    }

    public ChatOption setTextures(ResourceLocation textures) {
        if (textures != null) this.texture = textures;
        return this;
    }

    public ChatOption setTextures(String textures) {
        if (textures != null) return setTextures(ResourceLocation.parse(textures));
        return this;
    }

    public ChatOption setSelectTexture(ResourceLocation selectTexture) {
        if (selectTexture != null) this.selectTexture = selectTexture;
        return this;
    }

    public ChatOption setSelectTexture(String selectTexture) {
        if (selectTexture != null) return setSelectTexture(ResourceLocation.parse(selectTexture));
        return this;
    }

    public ChatOption setLockTexture(ResourceLocation lockTexture) {
        if (lockTexture != null) this.lockTexture = lockTexture;
        return this;
    }

    public ChatOption setLockTexture(String lockTexture) {
        if (lockTexture != null) return setLockTexture(ResourceLocation.parse(lockTexture));
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
                        minecraft.player.connection.sendCommand(value);
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

    public ChatOption setOptionChatPosition(int x, int y) {
        if (isResponsiveSkew(x) && isResponsiveSkew(y)) {
            this.optionChatX = x;
            this.optionChatY = y;
        }
        return this;
    }

    public ChatOption setOptionChatPosition(Integer[] optionChatPosition) {
        if (optionChatPosition != null) return setOptionChatPosition(optionChatPosition[0], optionChatPosition[1]);
        return this;
    }

    public ChatOption setTextAlign(TextAlign textAlign) {
        if (textAlign != null) this.textAlign = textAlign;
        return this;
    }

    public void click() {
        if (!this.isLock && minecraft.player != null) {
            //触发自定义事件
            this.onClickEvent.run();
            //跳转到指定的对话或者其它模块的对话
            if (StrUtil.isEmpty(this.next)){
                //跳转下一句话
                ChatBoxUtil.skipDialogues(minecraft.player.getUUID(),this.dialoguesResourceLocation,this.group,this.index+1);
            }else if (StrUtil.isInteger(this.next)) {
                //如果为数字跳转到指定序号的对话
                int index = Integer.parseInt(this.next);
                ChatBoxUtil.skipDialogues(minecraft.player.getUUID(), this.dialoguesResourceLocation, this.group, index);
            } else {
                //如果是英文则跳转到指定模块的对话
                ChatBoxUtil.skipDialogues(minecraft.player.getUUID(), this.dialoguesResourceLocation, this.next);
            }

        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Vec2 pos = getCurrentPosition();
        int x = (int) pos.x;
        int y = (int) pos.y;
        int color = CommonColors.WHITE;
        ResourceLocation texture = this.texture;
        if (this.isLock) {
            texture = this.lockTexture;
            color = CommonColors.GRAY;
        } else if (isSelect(mouseX, mouseY)) {
            texture = this.selectTexture;
            color = CommonColors.YELLOW;
        }

        //render image
        renderImage(guiGraphics, texture);

        //render option text
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        switch (this.textAlign) {
            case LEFT ->
                    guiGraphics.drawString(minecraft.font, parseText(optionChat.getString()), getResponsiveWidth(x + this.width / 2 + this.optionChatX), getResponsiveHeight(y + this.height / 2 + this.optionChatY), color);
            case CENTER ->
                    guiGraphics.drawCenteredString(minecraft.font, parseText(optionChat.getString()), getResponsiveWidth(x + this.width / 2 + this.optionChatX), getResponsiveHeight(y + this.height / 2 + this.optionChatY), color);
        }
        poseStack.popPose();

        //render tooltip
        if (!this.optionTooltip.getString().isEmpty() && isSelect(mouseX, mouseY)) {
            guiGraphics.renderTooltip(minecraft.font, this.optionTooltip, mouseX, mouseY);
        }
    }

    public enum TextAlign {
        LEFT,
        CENTER;

        public static TextAlign of(String text) {
            if (text == null) return TextAlign.LEFT;
            return valueOf(text.toUpperCase());
        }
    }
}
