package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.api.EventExecutor;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix3x2fStack;

public class ChatOption extends AbstractComponent<ChatOption> {
    //默认材质
    public Identifier texture;
    //鼠标悬浮材质
    public Identifier selectTexture;
    //上锁材质
    public Identifier lockTexture;
    //选项文本
    public String optionChat = "";
    //选项x位置
    public float optionChatX = 0;
    //选项y位置
    public float optionChatY = 0;
    //点击后触发内容
    public Runnable onClickEvent = () -> {};
    //是否上锁
    public boolean isLock = false;
    //解锁命令，若解锁命令不为null，则客户端设置完选项后会执行命令，若命令测试通过，选项会是正常可选状态
    //若命令测试不通过，如果原本isLock为true，则选项锁定，否则隐藏选项
    public String unlockCommand;
    //悬浮字体
    public String optionTooltip = "";
    //文本对齐
    public AlignX textAlign = AlignX.LEFT;
    //选项连接的下一个对话
    public String next;
    //记录选项原始y位置
    public float originY;
    //选项在chatBoxScreen被渲染时的索引，小于0不渲染也不能点击（隐藏）
    public int renderIndex = 0;

    public ChatOption() {
        setTextures(ChatBox.id("textures/options/default_no_checked_option.png"));
        setSelectTexture(ChatBox.id("textures/options/default_checked_option.png"));
        setLockTexture(ChatBox.id("textures/options/default_no_checked_option.png"));
    }

    @Override
    public ChatOption setPosition(float x, float y) {
        this.originY = y;
        return super.setPosition(x, y);
    }

    public ChatOption setOptionChat(String optionChat) {
        if (optionChat != null) this.optionChat = RenderUtil.translated(optionChat);
        return this;
    }

    public ChatOption setOptionTooltip(String optionTooltip) {
        if (optionTooltip != null) this.optionTooltip = RenderUtil.translated(optionTooltip);
        return this;
    }

    public ChatOption setTextures(Identifier textures) {
        if (textures != null) this.texture = textures;
        return this;
    }

    public ChatOption setTextures(String textures) {
        if (textures != null) return setTextures(Identifier.tryParse(textures));
        return this;
    }

    public ChatOption setSelectTexture(Identifier selectTexture) {
        if (selectTexture != null) this.selectTexture = selectTexture;
        return this;
    }

    public ChatOption setSelectTexture(String selectTexture) {
        if (selectTexture != null) return setSelectTexture(Identifier.tryParse(selectTexture));
        return this;
    }

    public ChatOption setLockTexture(Identifier lockTexture) {
        if (lockTexture != null) this.lockTexture = lockTexture;
        return this;
    }

    public ChatOption setLockTexture(String lockTexture) {
        if (lockTexture != null) return setLockTexture(Identifier.tryParse(lockTexture));
        return this;
    }

    public ChatOption setClickEvent(String type, String value) {
        if (type != null) {
            this.onClickEvent = () -> EventExecutor.executeEvent(this, type, value);
        }
        return this;
    }

    public ChatOption setIsLock(boolean isLock) {
        this.isLock = isLock;
        return this;
    }

    public ChatOption setUnlockCommand(String unlockCommand) {
        // 虽然execute也可以执行任意命令，但是为了不让玩家随意通过解锁命令执行任意命令，还是加个判断吧
        if (unlockCommand != null && unlockCommand.startsWith("execute"))
            this.unlockCommand = ChatBoxUtil.parseTargetPlaceholders(unlockCommand);
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

    public ChatOption setTextAlign(String textAlign) {
        if (textAlign != null) this.textAlign = AlignX.of(textAlign);
        return this;
    }

    /**@return 是否成功点击*/
    public boolean click() {
        if (this.renderIndex < 0 || this.hidden) return false;
        if (!this.isLock && minecraft.player != null) {
            //触发自定义事件
            this.onClickEvent.run();
            EventExecutor.executeEvent(null, "JUMP", this.next);
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        if (this.renderIndex < 0) return;
        super.render(guiGraphics, mouseX, mouseY, pPartialTick);
        this.y = this.originY + this.renderIndex * this.height;
        int num = ChatBoxUtil.chatBoxScreen.getRenderOptionCount();
        if (this.alignY == AlignY.CENTER) this.y -= (num - 1) * this.height / 2.0F;
        if (this.alignY == AlignY.BOTTOM) this.y -= (num - 1) * this.height;

        Vec2 pos = getCurrentPosition();
        float x = pos.x;
        float y = pos.y;
        int color = CommonColors.WHITE;
        Identifier texture = this.texture;
        if (this.isLock) {
            texture = this.lockTexture;
            color = CommonColors.GRAY;
        } else if (isSelect) {
            texture = this.selectTexture;
            color = CommonColors.YELLOW;
        }

        //render image
        if (texture != null) renderImage(guiGraphics, texture);

        //render option text
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        int responsiveX = (int) getResponsiveWidth(x + this.optionChatX);
        int responsiveY = (int) getResponsiveHeight(y + this.height / 2 + this.optionChatY) - 4; // 减去文本高度的一半
        int optionWidth = (int) getResponsiveWidth(this.width);
        RenderUtil.drawStringAlign(guiGraphics, parseText(this.optionChat), responsiveX, responsiveY, optionWidth, this.textAlign, color, false);
        poseStack.popMatrix();

        //render tooltip
        if (!this.optionTooltip.isEmpty() && isSelect) {
            RenderUtil.renderTooltip(guiGraphics, Component.nullToEmpty(this.optionTooltip), responsiveX, responsiveY);
        }
    }
}
