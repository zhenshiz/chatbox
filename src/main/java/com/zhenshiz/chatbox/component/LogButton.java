package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class LogButton extends AbstractComponent<LogButton> {
    public ResourceLocation logTexture;
    public ResourceLocation hoverLogTexture;

    public LogButton setLogTexture(ResourceLocation logTexture) {
        if (logTexture != null) this.logTexture = logTexture;
        return this;
    }

    public LogButton setLogTexture(String logTexture) {
        if (logTexture != null) setLogTexture(ResourceLocation.parse(logTexture));
        return this;
    }

    public LogButton setHoverLogTexture(ResourceLocation hoverLogTexture) {
        if (hoverLogTexture != null) this.hoverLogTexture = hoverLogTexture;
        return this;
    }

    public LogButton setHoverLogTexture(String hoverLogTexture) {
        if (hoverLogTexture != null) setHoverLogTexture(ResourceLocation.parse(hoverLogTexture));
        return this;
    }

    public void click() {
        if (minecraft.player != null) {
            minecraft.setScreen(ChatBoxUtil.historicalDialogue.get(minecraft.player.getUUID()));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isSelect(mouseX, mouseY)) {
            if (this.hoverLogTexture != null) renderImage(guiGraphics, this.hoverLogTexture);
        } else {
            if (this.logTexture != null) renderImage(guiGraphics, this.logTexture);
        }

    }
}
