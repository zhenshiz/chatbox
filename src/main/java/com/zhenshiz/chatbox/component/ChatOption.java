package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.Attachment;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;

public class ChatOption extends Portrait<ChatOption> {
    //默认材质
    public static final ResourceLocation root = ChatBox.ResourceLocationMod("textures/options/default_no_checked_option.png");
    //鼠标悬浮材质
    public static final ResourceLocation hover = ChatBox.ResourceLocationMod("textures/options/default_checked_option.png");
    //上锁材质
    public static final ResourceLocation lock = ChatBox.ResourceLocationMod("textures/options/default_lock_checked_option.png");
    //选项文本
    public String optionChat = "";
    //选项x位置
    public float optionChatX = 0;
    //选项y位置
    public float optionChatY = 0;
    //是否上锁
    public boolean isLock = false;
    //解锁命令，若解锁命令不为null，则客户端设置完选项后会执行命令，若命令测试通过，选项会是正常可选状态
    //若命令测试不通过，如果原本isLock为true，则选项锁定，否则隐藏选项
    public String unlockCommand;
    //悬浮字体
    public String optionTooltip = "";
    //文本对齐
    public AlignX textAlign = AlignX.LEFT;
    //记录选项原始y位置
    public float originY = 0;
    //选项在chatBoxScreen被渲染时的索引，小于0不渲染也不能点击（隐藏）
    public int renderIndex = 0;
    {
        this.id = "options";
    }

    @Override
    public ChatOption setPosition(Float x, Float y) {
        if (notNull(y)) this.originY = y;
        return super.setPosition(x, y);
    }

    public ChatOption setOptionChat(String optionChat) {
        if (notNull(optionChat)) this.optionChat = RenderUtil.translated(optionChat);
        return this;
    }

    public ChatOption setOptionTooltip(String optionTooltip) {
        if (notNull(optionTooltip)) this.optionTooltip = RenderUtil.translated(optionTooltip);
        return this;
    }

    public ChatOption setClickEvent(String type, String value) {
        if (notNull(type) && !type.equals("JUMP")) addEvent("ON_CLICK", type, value);
        return this;
    }

    public ChatOption setIsLock(Boolean isLock) {
        if (notNull(isLock)) this.isLock = isLock;
        return this;
    }

    public ChatOption setUnlockCommand(String unlockCommand) {
        // 虽然execute也可以执行任意命令，但是为了不让玩家随意通过解锁命令执行任意命令，还是加个判断吧
        if (notNull(unlockCommand) && unlockCommand.startsWith("execute"))
            this.unlockCommand = ChatBoxUtil.parseTargetPlaceholders(unlockCommand);
        return this;
    }

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
    public ResourceLocation getTexture() {return BeanUtil.getValueOrDefault(super.getTexture(), root);}

    @Override
    public ResourceLocation getHoverTexture() {return BeanUtil.getValueOrDefault(getTexture(HOVER), hover);}

    public ChatOption setLockTexture(String texture) {return setTexture("lock", texture);}

    public ResourceLocation getLockTexture() {return BeanUtil.getValueOrDefault(getTexture("lock"), lock);}

    /**@return 是否成功点击*/
    public boolean click() {
        if (this.renderIndex < 0 || this.hidden) return false;
        if (!this.isLock && minecraft.player != null) {
            //触发自定义事件
            fireEvent("ON_CLICK");
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        if (this.renderIndex < 0) return;
        renderInner(mouseX, mouseY);
        this.y = this.originY + this.renderIndex * this.height;
        int num = ChatBoxUtil.chatBoxScreen.getRenderOptionCount();
        if (this.alignY == AlignY.CENTER) this.y -= (num - 1) * this.height / 2.0F;
        if (this.alignY == AlignY.BOTTOM) this.y -= (num - 1) * this.height;

        int color = CommonColors.WHITE;
        ResourceLocation texture = getTexture();
        if (this.isLock) {
            texture = getLockTexture();
            color = CommonColors.GRAY;
        } else if (isSelect) {
            texture = getHoverTexture();
            color = CommonColors.YELLOW;
        }

        //render image
        renderImage(guiGraphics, texture, addTempAttachment(Attachment.ofText(this.optionChat, this.optionChatX, this.optionChatY, this.width, this.textAlign.name(), color, false)));

        //render tooltip
        if (!this.optionTooltip.isEmpty() && isSelect) {
            guiGraphics.renderTooltip(minecraft.font, Component.nullToEmpty(this.optionTooltip), (int) realX(), (int) realY());
        }
    }
}
