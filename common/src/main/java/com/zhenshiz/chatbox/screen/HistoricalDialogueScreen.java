package com.zhenshiz.chatbox.screen;

import com.mojang.blaze3d.platform.Window;
import com.zhenshiz.chatbox.component.HistoricalDialogue;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HistoricalDialogueScreen extends Screen {
    public HistoricalDialogue historicalDialogue;

    public HistoricalDialogueScreen() {
        super(Component.nullToEmpty("historicalDialogue"));
        Window window = Minecraft.getInstance().getWindow();
        historicalDialogue = new HistoricalDialogue(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    public HistoricalDialogueScreen setHistoricalDialogue(HistoricalDialogue historicalDialogue) {
        if (historicalDialogue != null) this.historicalDialogue = historicalDialogue;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            //右键关闭历史记录页面（想做成鼠标滚轮向下滚到底之后关闭的，暂时不会写）
            if (minecraft != null) {
                minecraft.setScreen(ChatBoxUtil.chatBoxScreen);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.setScreen(ChatBoxUtil.chatBoxScreen);
        }
        return false;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(historicalDialogue);
    }
}
