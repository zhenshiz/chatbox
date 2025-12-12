package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;

public class FunctionalButton extends AbstractComponent<FunctionalButton> {
    //默认材质
    public Identifier texture;
    //鼠标悬浮时材质
    public Identifier hoverTexture;
    //按钮类型
    public final Type type;
    private final ChatBoxScreen chatBoxScreen = ChatBoxUtil.chatBoxScreen;

    public FunctionalButton(Type type) {
        this.type = type;
        switch (type) {
            case LOG -> {
                setTexture(ChatBox.id("textures/button/default_log.png"));
                setHoverTexture(ChatBox.id("textures/button/default_hover_log.png"));
            }
            case FASTFORWARD -> {
                setTexture(ChatBox.id("textures/button/default_fastforward.png"));
                setHoverTexture(ChatBox.id("textures/button/default_hover_fastforward.png"));
            }
            case AUTOPLAY -> {
                setTexture(ChatBox.id("textures/button/default_autoplay.png"));
                setHoverTexture(ChatBox.id("textures/button/default_hover_autoplay.png"));
            }
        }
    }

    public FunctionalButton setTexture(Identifier texture) {
        if (texture != null) this.texture = texture;
        return this;
    }

    public FunctionalButton setTexture(String texture) {
        if (texture != null) setTexture(Identifier.tryParse(texture));
        return this;
    }

    public FunctionalButton setHoverTexture(Identifier hoverTexture) {
        if (hoverTexture != null) this.hoverTexture = hoverTexture;
        return this;
    }

    public FunctionalButton setHoverTexture(String hoverTexture) {
        if (hoverTexture != null) setHoverTexture(Identifier.tryParse(hoverTexture));
        return this;
    }

    /**@return 是否成功点击*/
    public boolean click() {
        if (minecraft.player != null) {
            switch (type) {
                case LOG -> minecraft.setScreen(ChatBoxUtil.historicalDialogue);
                //快进在玩家有任何左键或滚动鼠标滚轮操作之后停止（虽然但是，点快进按钮不行，懒得修了）
                case FASTFORWARD -> {
                    chatBoxScreen.fastForward = true;
                    chatBoxScreen.autoPlay = false;
                }
                //自动播放只在玩家手动点击这个按钮或快进按钮之后才停止
                case AUTOPLAY -> chatBoxScreen.autoPlay = !chatBoxScreen.autoPlay;
            }
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        super.render(guiGraphics, mouseX, mouseY, pPartialTick);
        Identifier texture = this.texture;
        if (isSelect) texture = hoverTexture;
        if (type == Type.AUTOPLAY && chatBoxScreen.autoPlay) texture = hoverTexture;
        if (type == Type.FASTFORWARD && chatBoxScreen.fastForward) texture = hoverTexture;
        if (texture != null) renderImage(guiGraphics, texture);

        if (isSelect) {
            String key = switch (type) {
                case LOG -> "chatbox.button.log";
                case FASTFORWARD -> "chatbox.button.fast_forward";
                case AUTOPLAY -> "chatbox.button.autoplay";
            };
            Vec2 position = getCurrentPosition();
            RenderUtil.drawStringAlign(guiGraphics, RenderUtil.translated(key), (int) getResponsiveWidth(position.x), (int) getResponsiveHeight(position.y) - 12, (int) getResponsiveWidth(this.width), this.alignX, -1, false);
        }
    }

    public enum Type {
        LOG,
        FASTFORWARD,
        AUTOPLAY;

        public static Type of(String type) {
            return valueOf(type.toUpperCase());
        }
    }
}
