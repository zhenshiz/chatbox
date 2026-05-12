package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.chatBoxScreen;

public class FunctionalButton extends Portrait<FunctionalButton> {
    //按钮类型
    public final Type type;

    public static final String
            log         = "chatbox:textures/button/default_log.png",
            fast        = "chatbox:textures/button/default_fastforward.png",
            auto        = "chatbox:textures/button/default_autoplay.png",
            log_hover   = "chatbox:textures/button/default_hover_log.png",
            fast_hover  = "chatbox:textures/button/default_hover_fastforward.png",
            auto_hover  = "chatbox:textures/button/default_hover_autoplay.png";

    public FunctionalButton(String type) {
        this.type = Type.of(type);
        setId(this.type.name().toLowerCase());
    }

    @Override
    public String getTexture() {
        var texture = super.getTexture();
        if (notNull(texture)) return texture;
        return switch (type) {
            case LOG -> log;
            case FASTFORWARD -> fast;
            case AUTOPLAY -> auto;
        };
    }

    @Override
    public String getHoverTexture() {
        var texture = getTexture(HOVER);
        if (notNull(texture)) return texture;
        return switch (type) {
            case LOG -> log_hover;
            case FASTFORWARD -> fast_hover;
            case AUTOPLAY -> auto_hover;
        };
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
    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        renderInner(mouseX, mouseY);
        String texture = getRenderTexture();
        if (isSelect || type == Type.AUTOPLAY && chatBoxScreen.autoPlay
                || type == Type.FASTFORWARD && chatBoxScreen.fastForward) texture = getHoverTexture();
        renderImage(guiGraphics, ChatBox.parseId(texture), getAttachments());

        if (isSelect) {
            String key = switch (type) {
                case LOG -> "chatbox.button.log";
                case FASTFORWARD -> "chatbox.button.fast_forward";
                case AUTOPLAY -> "chatbox.button.autoplay";
            };
            RenderUtil.drawStringAlign(guiGraphics, RenderUtil.translated(key), (int) realX(), (int) realY() - 12, (int) realWidth(), this.alignX, -1, false);
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
