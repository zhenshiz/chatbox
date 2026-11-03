package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class KeyPromptRender extends AbstractComponent<KeyPromptRender> {
    public static final ResourceLocation
            mouse_right = ChatBox.id("textures/key/right_mouse.png"),
            mouse_scroll = ChatBox.id("textures/key/scroll_mouse.png");
    public boolean visible;
    public float mouseTextureWidth;
    public float mouseTextureHeight;
    public ResourceLocation rightClickTexture;
    public ResourceLocation scrollTexture;
    public static final String ctrl = Minecraft.ON_OSX ? "Cmd" : "Ctrl";

    static Component keyRightClick;
    static Component keyScroll;
    static Component keyEsc;
    static Component keyCtrl;
    static Component keyF6;
    static long lastCacheTime;
    static float cachedWidth;

    public static void onLanguageChanged() {
        keyRightClick = Component.translatable("chatbox.key.right_click");
        keyScroll = Component.translatable("chatbox.key.scroll");
        keyEsc = Component.translatable("chatbox.key.esc");
        keyCtrl = Component.translatable("chatbox.key.ctrl");
        keyF6 = Component.translatable("chatbox.key.f6");
    }

    @Override
    public String getId() {return "key_prompt";}

    public KeyPromptRender setMouseTextureSize(Float width, Float height) {
        if (width != null) this.mouseTextureWidth = width;
        if (height != null) this.mouseTextureHeight = height;
        return this;
    }

    public KeyPromptRender setVisible(Boolean visible) {
        if (visible != null) this.visible = visible;
        return this;
    }

    public KeyPromptRender setRightClickTexture(String rightClickTexture) {
        if (rightClickTexture != null) this.rightClickTexture = ChatBox.parseId(rightClickTexture);
        return this;
    }

    public KeyPromptRender setScrollTexture(String scrollTexture) {
        if (scrollTexture != null) this.scrollTexture = ChatBox.parseId(scrollTexture);
        return this;
    }

    @Override
    public float realWidth() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheTime < 1000) return cachedWidth;
        lastCacheTime = currentTime;

        Font font = minecraft.font;
        cachedWidth = 2 * this.mouseTextureWidth + 8
                + font.width(keyRightClick) + font.width(keyScroll)
                + 10 + font.width("Esc") + font.width(keyEsc)
                + 6 + font.width("F6") + font.width(keyF6);
        if (ChatBoxUtil.chatBoxScreen.fastForwardButton() != null) {
            cachedWidth += 10 + font.width(ctrl) + font.width(keyCtrl);
        }
        return cachedWidth;
    }

    @Override
    public float realHeight() {
        return Math.max(this.mouseTextureHeight + 2, (float) minecraft.font.lineHeight / 2 + 12);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        if (!visible) return;
        renderInner(mouseX, mouseY);
        RenderUtil.renderOpacity(guiGraphics, brightness, opacity, () -> {
            guiGraphics.pose().pushPose();
            Font font = minecraft.font;

            int x = (int) realX();
            int y = (int) realY();
            guiGraphics.pose().last().pose().scaleAround(scale, x + realWidth() / 2, y + realHeight() / 2, 0);
            int textY = y + font.lineHeight / 2;

            //right click
            RenderUtil.renderImage(guiGraphics, BeanUtil.getValueOrDefault(this.rightClickTexture, mouse_right), x, y + 2, mouseTextureWidth, mouseTextureHeight, 1, 0f);
            drawText(guiGraphics, (int) (x + mouseTextureWidth + 2), textY, keyRightClick);
            x += (int) (mouseTextureWidth + font.width(keyRightClick) + 4);

            //mouse scroll
            RenderUtil.renderImage(guiGraphics, BeanUtil.getValueOrDefault(this.scrollTexture, mouse_scroll), x, y + 2, mouseTextureWidth, mouseTextureHeight, 1, 0f);
            drawText(guiGraphics, (int) (x + mouseTextureWidth + 2), textY, keyScroll);
            x += (int) (mouseTextureWidth + font.width(keyScroll) + 4);

            //esc
            drawKeyBoardKey(guiGraphics, x, textY, "Esc", false);
            drawText(guiGraphics, x + font.width("Esc") + 6, textY, keyEsc);
            x += 10 + font.width("Esc") + font.width(keyEsc);

            //ctrl
            if (ChatBoxUtil.chatBoxScreen.fastForwardButton() != null) {
                drawKeyBoardKey(guiGraphics, x, textY, ctrl, false);
                drawText(guiGraphics, x + font.width(ctrl) + 6, textY, keyCtrl);
                x += 10 + font.width(ctrl) + font.width(keyCtrl);
            }

            //f6
            drawKeyBoardKey(guiGraphics, x, textY, "F6", ChatBoxUtil.chatBoxScreen.autoPlay);
            drawText(guiGraphics, x + font.width("F6") + 6, textY, keyF6);
            guiGraphics.pose().popPose();
        });
    }

    public static void drawKeyBoardKey(GuiGraphics guiGraphics, int x, int y, String key, boolean pressed) {
        Font font = minecraft.font;

        // 按键尺寸
        int width = font.width(key) + 4;
        int height = 12;

        int topColor = 0xFF707070;  // 上亮面
        int faceColor = 0xFF505050;  // 主体灰色
        int bottomColor = 0xFF202020;  // 下阴影

        if (pressed) {
            topColor = 0xFF505050;
            faceColor = 0xFF202020;
            bottomColor = 0xFF000000;
        }

        // 背景
        guiGraphics.fillGradient(x, y, x + width, y + height, topColor, bottomColor); // 垂直渐变背景
        guiGraphics.fill(x, y, x + width, y + height, faceColor); // 覆盖主色

        // 画边框
        guiGraphics.fill(x, y, x + width, y + 1, topColor); // 顶部线
        guiGraphics.fill(x, y, x + 1, y + height, topColor); // 左边线
        guiGraphics.fill(x, y + height - 1, x + width, y + height, bottomColor); // 底部线
        guiGraphics.fill(x + width - 1, y, x + width, y + height, bottomColor); // 右边线

        guiGraphics.drawCenteredString(font, key, x + width / 2, y + (height - 8) / 2, 0xFFFFFFFF);
    }

    public static void drawText(GuiGraphics guiGraphics, int x, int y, Component text) {
        Font font = minecraft.font;
        // 渲染文字描边（四周偏移1像素）
        guiGraphics.drawString(font, text, x - 1, y, 0xFF000000, true);
        guiGraphics.drawString(font, text, x + 1, y, 0xFF000000, true);
        guiGraphics.drawString(font, text, x, y - 1, 0xFF000000, true);
        guiGraphics.drawString(font, text, x, y + 1, 0xFF000000, true);

        // 渲染主体文字
        guiGraphics.drawString(font, text, x, y, 0xFFFFFFFF, true);
    }
}
