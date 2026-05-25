package com.zhenshiz.chatbox.client.screen;

import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4i;

import java.util.ArrayList;
import java.util.List;

public class HistoricalDialogue extends AbstractWidget {
    public List<HistoricalInfo> historicalInfos = new ArrayList<>();
    private float yOffset = 0;
    private float verticalScrollAmount = 0;
    private static final Minecraft minecraft = Minecraft.getInstance();

    public HistoricalDialogue(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public void clearHistory() {
        historicalInfos.clear();
        verticalScrollAmount = 0;
    }

    @Override
    protected void extractWidgetRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        yOffset = Math.clamp(yOffset + verticalScrollAmount * delta, height - 30 - (minecraft.font.lineHeight + 27) * historicalInfos.size(), height - 30);
        if (verticalScrollAmount > 0) {
            verticalScrollAmount = Math.clamp(verticalScrollAmount - delta * 3, 0, Float.MAX_VALUE);
        } else {
            verticalScrollAmount = Math.clamp(verticalScrollAmount + delta * 3, -Float.MAX_VALUE, 0);
        }
        int y = (int) (yOffset - 9);
        for (var historicalInfo : historicalInfos) {
            y += minecraft.font.lineHeight + 27;
            if (y < -36 || y > RenderUtil.screenHeight()) continue;
            historicalInfo.render(guiGraphics, y, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float direction = (float) Math.signum(verticalAmount);
        float baseSpeed = ChatBoxClient.conf.historicalScrollSpeed /*Config.historicalScrollSpeed.get()*/;
        verticalScrollAmount = direction * baseSpeed;
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (mouseButtonEvent.button() == 0 && ChatBoxUtil.chatBoxScreen.isHistoricalSkip) {
            for (HistoricalInfo historicalInfo : historicalInfos) {
                if (historicalInfo.isMouseInRect(mouseButtonEvent.x(), mouseButtonEvent.y())) {
                    int i = historicalInfos.indexOf(historicalInfo);
                    //清除该index之后的所有记录
                    historicalInfos = historicalInfos.subList(0, i);
                    historicalInfo.click();
                    return super.mouseClicked(mouseButtonEvent, bl);
                }
            }
        }
        return super.mouseClicked(mouseButtonEvent, bl);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

    }

    public static class HistoricalInfo {
        public String name;
        public String text;
        public Identifier identifier;
        public String group;
        public int index;

        private Vector4i vector4i;
        private float progress;

        public HistoricalInfo(Identifier identifier, String group, int index, String name, String text) {
            this.name = RenderUtil.translated(name);
            this.text = RenderUtil.translated(text);
            this.identifier = identifier;
            this.group = group;
            this.index = index;
        }

        private void render(GuiGraphicsExtractor guiGraphics, int y, double mouseX, double mouseY, float delta) {
            Vector4i rect = createEntryRect(y);
            this.vector4i = rect;
            int x1 = rect.x; int y1 = rect.y; int x2 = rect.z; int y2 = rect.w;
            var poseStack = guiGraphics.pose();
            poseStack.pushMatrix();
            poseStack.scaleAround(1 + 0.05F * progress, (float) (x1 + x2) / 2, (float) (y1 + y2) / 2);
            boolean inRect = isMouseInRect(mouseX, mouseY);
            this.progress = Math.clamp(progress + (inRect ? delta * 0.5F : -delta * 0.5F), 0, 1);
            guiGraphics.fill(x1, y1, x2, y2, getBackgroundColor());
            int width = RenderUtil.screenWidth() / 7 * 5;
            var font = minecraft.font;
            if (CollUtil.notEmpty(this.name)) guiGraphics.textWithWordWrap(font, Component.literal(StrUtil.maxLength(ChatBoxUtil.parseText(this.name, false), 60)), x1 + 3, y1 + 3, width, CommonColors.WHITE);
            if (CollUtil.notEmpty(this.text)) guiGraphics.textWithWordWrap(font, Component.literal(StrUtil.maxLength(ChatBoxUtil.parseText(this.text, false), 60)), x1 + 3, y1 + 16, width, CommonColors.WHITE);
            poseStack.popMatrix();
        }

        public void click() {
            ChatBoxUtil.skipDialogues(identifier, group, index);
        }

        public boolean isMouseInRect(double mouseX, double mouseY) {
            return (this.vector4i.x <= mouseX && mouseX <= this.vector4i.z) && (this.vector4i.y <= mouseY && mouseY <= this.vector4i.w);
        }

        private Vector4i createEntryRect(int yOffset) {
            int width = RenderUtil.screenWidth();
            return new Vector4i(width / 7, -7 + yOffset, width / 7 * 6, 29 + yOffset);
        }

        private int getBackgroundColor() {
            return ((int) (0x22 + (0x88 - 0x22) * progress) << 24) | 0xFFFFFF;
        }
    }
}
