package com.zhenshiz.chatbox.client.screen;

import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class HistoricalDialogueScreen extends Screen {
    public final HistoricalDialogue historicalDialogue;

    public HistoricalDialogueScreen() {
        super(Component.nullToEmpty("historicalDialogue"));
        historicalDialogue = new HistoricalDialogue(0, 0, RenderUtil.screenWidth(), RenderUtil.screenHeight());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        //右键关闭历史记录页面（想做成鼠标滚轮向下滚到底之后关闭的，暂时不会写）
        if (mouseButtonEvent.button() == 1) onClose();
        return super.mouseClicked(mouseButtonEvent, bl);
    }

    @Override
    public void onClose() {if (ChatBoxUtil.isScreen) ChatBoxUtil.minecraft.setScreen(ChatBoxUtil.chatBoxScreen);}

    public void addHistoricalInfo(Identifier identifier, String group, int index, String name, String text) {
        historicalDialogue.historicalInfos.add(new HistoricalDialogue.HistoricalInfo(identifier, group, index, name, text));
    }

    @Override
    protected void init() {
        this.addRenderableWidget(historicalDialogue);
    }
}
