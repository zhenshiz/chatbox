package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.component.data.Attachment;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class ChatOption extends Portrait<ChatOption> {
    public static final String
            root = "chatbox:textures/options/default_no_checked_option.png",
            hover = "chatbox:textures/options/default_checked_option.png",
            lock = "chatbox:textures/options/default_lock_checked_option.png";
    //选项文本
    public String optionChat = "";
    //选项x位置
    public float optionChatX = 0;
    //选项y位置
    public float optionChatY = 0;
    //是否上锁
    public boolean isLock = false;
    //悬浮字体
    public String optionTooltip = "";
    //文本对齐
    public AlignX textAlign = AlignX.LEFT;
    //记录选项原始y位置
    public float originY = 0;
    //选项在chatBoxScreen被渲染时的索引，小于0不渲染也不能点击（隐藏）
    public int renderIndex = 0;

    @Override
    public String getId() {return "option" + ChatBoxUtil.chatBoxScreen.chatOptions.indexOf(this);}

    @Override
    public ChatOption setPosition(Float x, Float y) {
        if (notNull(y)) this.originY = y;
        return super.setPosition(x, y);
    }

    public ChatOption setOptionChat(String optionChat) {
        if (notNull(optionChat)) this.optionChat = optionChat;
        return this;
    }

    public ChatOption setOptionTooltip(String optionTooltip) {
        if (notNull(optionTooltip)) this.optionTooltip = parseText(RenderUtil.translated(optionTooltip));
        return this;
    }

    public ChatOption setClickEvent(String type, String value) {
        if (notNull(type) && !type.equalsIgnoreCase("JUMP")) addEvent("ON_CLICK", type, value);
        return this;
    }

    public ChatOption setCondition(String condition, boolean lockOrHide) {
        if (notNull(condition) && !condition.isEmpty()) {
            getOrCreateEvents().add("CHECK", condition, "SET_NORMAL", "@s", this);
            if (lockOrHide) setIsLock(true); else hideOption(true);
        }
        return this;
    }

    public void hideOption(Boolean hidden) {
        if (notNull(hidden)) {
            if (hidden) this.renderIndex = -1;
            else this.renderIndex = 0;
        }
    }

    public boolean hiddenByCommand() {return this.renderIndex < 0;}

    public ChatOption setNext(String next) {
        addEvent("ON_CLICK", "JUMP", notNull(next) ? next : "");
        return this;
    }

    public ChatOption setOptionChatPosition(Float x, Float y) {
        if (notNull(x)) this.optionChatX = x;
        if (notNull(y)) this.optionChatY = y;
        return this;
    }

    public ChatOption setTextAlign(String textAlign) {
        if (notNull(textAlign)) this.textAlign = AlignX.of(textAlign);
        return this;
    }

    @Override
    public String getTexture() {return BeanUtil.getValueOrDefault(super.getTexture(), root);}

    @Override
    public String getHoverTexture() {return BeanUtil.getValueOrDefault(getTexture(HOVER), hover);}

    @Override
    public String getLockTexture() {return BeanUtil.getValueOrDefault(getTexture(LOCK), lock);}

    /**@return 是否成功点击*/
    public boolean click() {
        if (hiddenByCommand() || this.hidden || this.isLock) return false;
        //触发自定义事件
        return fireEvent("ON_CLICK") > 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        if (hiddenByCommand()) return;
        renderInner(mouseX, mouseY);
        this.y = this.originY + this.renderIndex * this.height;
        int num = ChatBoxUtil.chatBoxScreen.getRenderOptionCount();
        if (this.alignY == AlignY.CENTER) this.y -= (num - 1) * this.height / 2.0F;
        if (this.alignY == AlignY.BOTTOM) this.y -= (num - 1) * this.height;

        int color = isLock ? CommonColors.GRAY : isSelect ? CommonColors.YELLOW : CommonColors.WHITE;
        //render image
        renderImage(guiGraphics, getRenderResource(), addTempAttachment(Attachment.ofText(this.optionChat, this.optionChatX, this.optionChatY, this.width, this.textAlign.name(), color, false, true)));

        //render tooltip
        if (!this.optionTooltip.isEmpty() && isSelect) {
            guiGraphics.renderTooltip(minecraft.font, Component.nullToEmpty(this.optionTooltip), (int) realX(), (int) realY());
        }
    }
}
