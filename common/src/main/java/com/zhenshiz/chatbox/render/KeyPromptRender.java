package com.zhenshiz.chatbox.render;

import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

public class KeyPromptRender extends AbstractComponent<KeyPromptRender> {
    public Boolean visible;
    public ResourceLocation rightClickTexture;
    public ResourceLocation scrollTexture;

    public KeyPromptRender setVisible(Boolean visible) {
        if (visible != null) this.visible = visible;
        return this;
    }

    public KeyPromptRender setRightClickTexture(String rightClickTexture) {
        if (rightClickTexture != null) this.rightClickTexture = new ResourceLocation(rightClickTexture);
        return this;
    }

    public KeyPromptRender setScrollTexture(String scrollTexture) {
        if (scrollTexture != null) this.scrollTexture = new ResourceLocation(scrollTexture);
        return this;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float pPartialTick) {
        if (this.visible) {
            RenderUtil.renderOpacity(guiGraphics, this.opacity, () -> {
                Font font = minecraft.font;

                String keyRightClick = Component.translatable("chatbox.key.right_click").getString();
                String keyScroll = Component.translatable("chatbox.key.scroll").getString();
                String keyEsc = Component.translatable("chatbox.key.esc").getString();
                String keyCtrl = Component.translatable("chatbox.key.ctrl").getString();
                String keyF6 = Component.translatable("chatbox.key.f6").getString();

                Vec2 vec2 = getCurrentPosition();
                float x = vec2.x;
                float y = vec2.y;

                //right scroll
                RenderUtil.renderImage(guiGraphics, BeanUtil.getValueOrDefault(this.rightClickTexture, new ResourceLocation("chatbox:textures/key/right_mouse.png")), x, y + 2, 0, 12, 16, 1);
                drawText(guiGraphics, x + 14, y + (float) font.lineHeight / 2, keyRightClick);

                x += 18 + font.width(keyRightClick);

                //mouse scroll
                RenderUtil.renderImage(guiGraphics, BeanUtil.getValueOrDefault(this.scrollTexture, new ResourceLocation("chatbox:textures/key/scroll_mouse.png")), x, y + 2, 0, 12, 16, 1);
                drawText(guiGraphics, x + 14, y + (float) font.lineHeight / 2, keyScroll);

                x += 18 + font.width(keyScroll);

                //esc
                drawKeyBoardKey(guiGraphics, (int) x, (int) y + font.lineHeight / 2, "Esc", false);
                drawText(guiGraphics, x + font.width("Esc") + 6, y + (float) font.lineHeight / 2, keyEsc);

                x += 10 + font.width("Esc") + font.width(keyEsc);

                //ctrl
                drawKeyBoardKey(guiGraphics, (int) (x), (int) y + font.lineHeight / 2, "Ctrl", false);
                drawText(guiGraphics, x + font.width("Ctrl") + 6, y + (float) font.lineHeight / 2, keyCtrl);

                x += 10 + font.width("Ctrl") + font.width(keyCtrl);

                //f6
                drawKeyBoardKey(guiGraphics, (int) (x), (int) y + font.lineHeight / 2, "F6", ChatBoxUtil.chatBoxScreen.autoPlay);
                drawText(guiGraphics, x + font.width("F6") + 6, y + (float) font.lineHeight / 2, keyF6);
            });
        }
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

    public static void drawText(GuiGraphics guiGraphics, float x, float y, String text) {
        Font font = Minecraft.getInstance().font;
        // 渲染文字描边（四周偏移1像素）
        guiGraphics.drawString(
                font,
                text,
                (int) (x - 1), (int) y,
                0xFF000000,
                true
        );
        guiGraphics.drawString(
                font,
                text,
                (int) (x + 1), (int) y,
                0xFF000000,
                true
        );
        guiGraphics.drawString(
                font,
                text,
                (int) x, (int) (y - 1),
                0xFF000000,
                true
        );
        guiGraphics.drawString(
                font,
                text,
                (int) x, (int) (y + 1),
                0xFF000000,
                true
        );

        // 渲染主体文字
        guiGraphics.drawString(
                font,
                text,
                (int) x, (int) y,
                0xFFFFFFFF,
                true
        );
    }
}
