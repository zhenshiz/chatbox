package com.zhenshiz.chatbox.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zhenshiz.chatbox.Config;
import com.zhenshiz.chatbox.screen.HistoricalDialogueScreen;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    public HistoricalDialogue addHistoricalInfo(HistoricalInfo historicalInfo) {
        if (historicalInfo != null) this.historicalInfos.add(historicalInfo);
        return this;
    }

    public HistoricalDialogue setHistoricalInfo(List<HistoricalInfo> historicalInfos) {
        if (historicalInfos != null) this.historicalInfos = historicalInfos;
        return this;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        yOffset = Math.clamp(yOffset + verticalScrollAmount * delta, height - 30 - (minecraft.font.lineHeight + 27) * historicalInfos.size(), height - 30);
        if (verticalScrollAmount > 0) {
            verticalScrollAmount = Math.clamp(verticalScrollAmount - delta * 3, 0, Float.MAX_VALUE);
        } else {
            verticalScrollAmount = Math.clamp(verticalScrollAmount + delta * 3, -Float.MAX_VALUE, 0);
        }
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 27 + yOffset, 0);
        for (HistoricalInfo historicalInfo : historicalInfos) {
            historicalInfo.render(this, guiGraphics, mouseX, mouseY, delta);
            poseStack.translate(0, minecraft.font.lineHeight + 27, 0);
        }
        poseStack.popPose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float direction = (float) Math.signum(verticalAmount);
        float baseSpeed = Config.historicalScrollSpeed.get();
        verticalScrollAmount = direction * baseSpeed;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (HistoricalInfo historicalInfo : historicalInfos) {
                if (historicalInfo.isMouseInRect(mouseX, mouseY)) {
                    historicalInfo.click();
                    return super.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

    }

    public static class HistoricalInfo {
        public Component name;
        public Component text;
        public ResourceLocation resourceLocation;
        public String group;
        public int index;

        private Vector4i vector4i;
        private float progress;

        public HistoricalInfo(ResourceLocation resourceLocation, String group, int index) {
            this.name = CommonComponents.EMPTY;
            this.text = CommonComponents.EMPTY;
            this.resourceLocation = resourceLocation;
            this.group = group;
            this.index = index;
        }

        public HistoricalInfo setName(String name, boolean isTranslatable) {
            if (name != null) this.name = isTranslatable ? Component.translatable(name) : Component.nullToEmpty(name);
            return this;
        }

        public HistoricalInfo setText(String text, boolean isTranslatable) {
            if (text != null) this.text = isTranslatable ? Component.translatable(text) : Component.nullToEmpty(text);
            return this;
        }

        private void render(HistoricalDialogue historicalDialogue, GuiGraphics guiGraphics, double mouseX, double mouseY, float delta) {
            this.vector4i = createEntryAbsoluteRect(guiGraphics);
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(historicalDialogue.width * -0.025F * progress, 0, 0);
            poseStack.scale(1 + 0.05F * progress, 1 + 0.05F * progress, 1 + 0.05F * progress);
            Vector4i relativelyRect = createEntryRelativelyRect();
            Font font = Minecraft.getInstance().font;
            boolean inRect = isMouseInRect(mouseX, mouseY);
            this.progress = Math.clamp(progress + (inRect ? delta * 0.5F : -delta * 0.5F), 0, 1);
            guiGraphics.fill(relativelyRect.x, relativelyRect.y, relativelyRect.z, relativelyRect.w, getBackgroundColor());
            int lineBreak = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 7 * 5;
            guiGraphics.drawWordWrap(font, Component.nullToEmpty(StrUtil.maxLength(ChatBoxUtil.parseText(this.name.getString(), true), 60)), relativelyRect.x + 3, -5, lineBreak, CommonColors.WHITE);
            guiGraphics.drawWordWrap(font, Component.nullToEmpty(StrUtil.maxLength(ChatBoxUtil.parseText(this.text.getString(), true), 60)), relativelyRect.x + 3, 8, lineBreak, CommonColors.WHITE);

            poseStack.popPose();
        }

        public void click() {
            if (minecraft.player != null) {
                if (minecraft.screen != null) {
                    minecraft.screen.onClose();
                }
                ChatBoxUtil.historicalDialogue= new HistoricalDialogueScreen();
                ChatBoxUtil.skipDialogues(resourceLocation, group, index);
            }
        }

        public boolean isMouseInRect(double mouseX, double mouseY) {
            return (this.vector4i.x <= mouseX && mouseX <= this.vector4i.z) && (this.vector4i.y <= mouseY && mouseY <= this.vector4i.w);
        }

        public Vector4i createEntryAbsoluteRect(GuiGraphics guiGraphics) {
            int y = (int) guiGraphics.pose().last().pose().m31();
            return createEntryRelativelyRect().add(0, y, 0, y);
        }

        private Vector4i createEntryRelativelyRect() {
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            return new Vector4i(width / 7, -7, width / 7 * 6, 20 + Minecraft.getInstance().font.lineHeight);
        }

        private int getBackgroundColor() {
            return ((int) (0x22 + (0x88 - 0x22) * progress) << 24) | 0xFFFFFF;
        }
    }
}
