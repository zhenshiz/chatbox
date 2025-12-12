package com.zhenshiz.chatbox.utils.chatbox;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenderUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();

    public static int screenWidth() {
        return minecraft.getWindow().getGuiScaledWidth();
    }

    public static int screenHeight() {
        return minecraft.getWindow().getGuiScaledHeight();
    }

    //fill

    //矩形
    public static void fillRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        buf.vertex(mat, (float) x, (float) y, 0).color(color).endVertex();
        buf.vertex(mat, (float) (x + w), (float) y, 0).color(color).endVertex();
        buf.vertex(mat, (float) (x + w), (float) (y + h), 0).color(color).endVertex();
        buf.vertex(mat, (float) x, (float) (y + h), 0).color(color).endVertex();

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    //圆弧
    public static void fillArc(GuiGraphics guiGraphics, int cX, int cY, int radius, int start, int end, int color) {
        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        buf.vertex(mat, (float) cX, (float) cY, 0).color(color).endVertex();

        for (int i = start - 90; i <= end - 90; i++) {
            double angle = Math.toRadians(i);
            float x = (float) (Math.cos(angle) * radius) + cX;
            float y = (float) (Math.sin(angle) * radius) + cY;
            buf.vertex(mat, x, y, 0).color(color).endVertex();
        }

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    //圆
    public static void fillCircle(GuiGraphics guiGraphics, int cX, int cY, int radius, int color) {
        fillArc(guiGraphics, cX, cY, radius, 0, 360, color);
    }

    //环形扇区
    public static void fillAnnulusArc(GuiGraphics guiGraphics, int cx, int cy, int radius, int start, int end, int thickness, int color) {
        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        for (int i = start - 90; i <= end - 90; i++) {
            float angle = (float) Math.toRadians(i);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float x1 = cx + cos * radius;
            float y1 = cy + sin * radius;
            float x2 = cx + cos * (radius + thickness);
            float y2 = cy + sin * (radius + thickness);
            buf.vertex(mat, x1, y1, 0).color(color).endVertex();
            buf.vertex(mat, x2, y2, 0).color(color).endVertex();
        }

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    //环形圆
    public static void fillAnnulus(GuiGraphics guiGraphics, int cx, int cy, int radius, int thickness, int color) {
        fillAnnulusArc(guiGraphics, cx, cy, radius, 0, 360, thickness, color);
    }

    //实心圆角矩形
    public static void fillRoundRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);

        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        buf.vertex(mat, x + w / 2F, y + h / 2F, 0).color(color).endVertex();

        int[][] corners = {
                {x + w - r, y + r},
                {x + w - r, y + h - r},
                {x + r, y + h - r},
                {x + r, y + r}
        };

        for (int corner = 0; corner < 4; corner++) {
            int cornerStart = (corner - 1) * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                buf.vertex(mat, rx, ry, 0).color(color).endVertex();
            }
        }

        buf.vertex(mat, corners[0][0], y, 0).color(color).endVertex();

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    //圆角阴影边框
    public static void fillRoundShadow(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int thickness, int innerColor, int outerColor) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);

        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        int[][] corners = {
                {x + w - r, y + r},
                {x + w - r, y + h - r},
                {x + r, y + h - r},
                {x + r, y + r}
        };

        for (int corner = 0; corner < 4; corner++) {
            int cornerStart = (corner - 1) * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx1 = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry1 = corners[corner][1] + (float) (Math.sin(angle) * r);
                float rx2 = corners[corner][0] + (float) (Math.cos(angle) * (r + thickness));
                float ry2 = corners[corner][1] + (float) (Math.sin(angle) * (r + thickness));
                buf.vertex(mat, rx1, ry1, 0).color(innerColor).endVertex();
                buf.vertex(mat, rx2, ry2, 0).color(outerColor).endVertex();
            }
        }

        buf.vertex(mat, corners[0][0], y, 0).color(innerColor).endVertex();
        buf.vertex(mat, corners[0][0], y - thickness, 0).color(outerColor).endVertex();

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    //上圆角矩形
    public static void fillRoundTabTop(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);

        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        buf.vertex(mat, x + w / 2F, y + h / 2F, 0).color(color).endVertex();

        int[][] corners = {
                {x + r, y + r},
                {x + w - r, y + r}
        };

        for (int corner = 0; corner < 2; corner++) {
            int cornerStart = (corner - 2) * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                buf.vertex(mat, rx, ry, 0).color(color).endVertex();
            }
        }

        buf.vertex(mat, x + w, y + h, 0).color(color).endVertex();
        buf.vertex(mat, x, y + h, 0).color(color).endVertex();
        buf.vertex(mat, x, corners[0][1], 0).color(color).endVertex(); // connect last to first vertex

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    //下圆角矩形
    public static void fillRoundTabBottom(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);

        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        buf.vertex(mat, x + w / 2F, y + h / 2F, 0).color(color).endVertex();

        int[][] corners = {
                {x + w - r, y + h - r},
                {x + r, y + h - r}
        };

        for (int corner = 0; corner < 2; corner++) {
            int cornerStart = corner * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                buf.vertex(mat, rx, ry, 0).color(color).endVertex();
            }
        }

        buf.vertex(mat, x, y, 0).color(color).endVertex();
        buf.vertex(mat, x + w, y, 0).color(color).endVertex();
        buf.vertex(mat, x + w, corners[0][1], 0).color(color).endVertex(); // connect last to first vertex

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    //水平方向的胶囊状线条
    public static void fillRoundHorLine(GuiGraphics guiGraphics, int x, int y, int length, int thickness, int color) {
        fillRoundRect(guiGraphics, x, y, length, thickness, thickness / 2, color);
    }

    //垂直方向的胶囊状线条
    public static void fillRoundVerLine(GuiGraphics guiGraphics, int x, int y, int length, int thickness, int color) {
        fillRoundRect(guiGraphics, x, y, thickness, length, thickness / 2, color);
    }

    //draw

    //矩形
    public static void drawRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        drawHorLine(guiGraphics, x, y, w, color);
        drawVerLine(guiGraphics, x, y + 1, h - 2, color);
        drawVerLine(guiGraphics, x + w - 1, y + 1, h - 2, color);
        drawHorLine(guiGraphics, x, y + h - 1, w, color);
    }

    //盒子
    public static void drawBox(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        drawLine(guiGraphics, x, y, x + w, y, color);
        drawLine(guiGraphics, x, y + h, x + w, y + h, color);
        drawLine(guiGraphics, x, y, x, y + h, color);
        drawLine(guiGraphics, x + w, y, x + w, y + h, color);
    }

    //横线
    public static void drawHorLine(GuiGraphics guiGraphics, int x, int y, int length, int color) {
        fillRect(guiGraphics, x, y, length, 1, color);
    }

    //竖线
    public static void drawVerLine(GuiGraphics guiGraphics, int x, int y, int length, int color) {
        fillRect(guiGraphics, x, y, 1, length, color);
    }

    //一条线
    public static void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        buf.vertex(mat, (float) x1, (float) y1, 0).color(color).endVertex();
        buf.vertex(mat, (float) x2, (float) y2, 0).color(color).endVertex();

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }


    //扇形
    public static void drawArc(GuiGraphics guiGraphics, int cX, int cY, int radius, int start, int end, int color) {
        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        for (int i = start - 90; i <= end - 90; i++) {
            double angle = Math.toRadians(i);
            float x = (float) (Math.cos(angle) * radius) + cX;
            float y = (float) (Math.sin(angle) * radius) + cY;
            buf.vertex(mat, x, y, 0).color(color).endVertex();
        }

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    //圆
    public static void drawCircle(GuiGraphics guiGraphics, int cX, int cY, int radius, int color) {
        drawArc(guiGraphics, cX, cY, radius, 0, 360, color);
    }

    //圆角矩形
    public static void drawRoundRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);

        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        int[][] corners = {
                {x + w - r, y + r},
                {x + w - r, y + h - r},
                {x + r, y + h - r},
                {x + r, y + r}
        };

        for (int corner = 0; corner < 4; corner++) {
            int cornerStart = (corner - 1) * 90;
            int cornerEnd = cornerStart + 90;
            for (int i = cornerStart; i <= cornerEnd; i += 10) {
                float angle = (float) Math.toRadians(i);
                float rx = corners[corner][0] + (float) (Math.cos(angle) * r);
                float ry = corners[corner][1] + (float) (Math.sin(angle) * r);
                buf.vertex(mat, rx, ry, 0).color(color).endVertex();
            }
        }

        buf.vertex(mat, corners[0][0], y, 0).color(color).endVertex(); // connect last to first vertex

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    //圆角横线
    public static void drawRoundHorLine(GuiGraphics guiGraphics, int x, int y, int length, int thickness, int color) {
        drawRoundRect(guiGraphics, x, y, length, thickness, thickness / 2, color);
    }

    //圆角竖线
    public static void drawRoundVerLine(GuiGraphics guiGraphics, int x, int y, int length, int thickness, int color) {
        drawRoundRect(guiGraphics, x, y, thickness, length, thickness / 2, color);
    }

    // image
    public static void renderImageInner(GuiGraphics guiGraphics, ResourceLocation resourceLocation, float x, float y, float z, float uw, float uh, float width, float height) {
        BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        buf.vertex(matrix4f, x, y, z).uv(0, 0).endVertex();
        buf.vertex(matrix4f, x, y + height, z).uv(0, uh).endVertex();
        buf.vertex(matrix4f, x + width, y + height, z).uv(uw, uh).endVertex();
        buf.vertex(matrix4f, x + width, y, z).uv(uw, 0).endVertex();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, resourceLocation);
        RenderSystem.enableBlend();
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.disableBlend();
    }

    public static void renderImage(GuiGraphics guiGraphics, ResourceLocation resourceLocation, float x, float y, float z, float width, float height, float scale, float angle, List<ChatBoxTheme.Portrait.Attachment> attachments) {
        guiGraphics.pose().pushPose();
        float centerX = x + width / 2;
        float centerY = y + height / 2;
        // 应用旋转
        guiGraphics.pose().rotateAround(new Quaternionf().fromAxisAngleDeg(0, 0, 1, angle), centerX, centerY, 0);
        guiGraphics.pose().last().pose().scaleAround(scale, centerX, centerY, 0);
        renderImageInner(guiGraphics, resourceLocation, x, y, z, 1, 1, width, height);
        for (var attachment : attachments) {
            var a = attachment.mapParameter();
            renderImageInner(guiGraphics, new ResourceLocation(a.value), x + a.x, y + a.y, z, 1, 1, a.width, a.height);
        }
        guiGraphics.pose().popPose();
    }

    public static void renderImage(GuiGraphics guiGraphics, ResourceLocation resourceLocation, float x, float y, float z, float width, float height, float scale, float angle) {
        renderImage(guiGraphics, resourceLocation, x, y, z, width, height, scale, angle, List.of());
    }

    public static void renderPlayerHead(GuiGraphics guiGraphics, String input, int x, int y, int size, float scale, float angle) {
        PoseStack pose = guiGraphics.pose();
        ResourceLocation skin = getSkin(input);
        pose.pushPose();
        float centerX = x + (float) size / 2;
        float centerY = y + (float) size / 2;
        // 应用旋转
        guiGraphics.pose().rotateAround(new Quaternionf().fromAxisAngleDeg(0, 0, 1, angle), centerX, centerY, 0);
        guiGraphics.pose().last().pose().scaleAround(scale, centerX, centerY, 0);
        guiGraphics.blit(skin, x, y, size, size, 8, 8, 8, 8, 64, 64);
        RenderSystem.enableBlend();
        guiGraphics.blit(skin, x - 1, y - 1, size + 2, size + 2, 40, 8, 8, 8, 64, 64);
        RenderSystem.disableBlend();
        pose.popPose();
    }

    public static void renderItem(GuiGraphics guiGraphics, ItemStack item, int x, int y, float scale, float angle, String text) {
        guiGraphics.pose().pushPose();
        float centerX = x + 8f;
        float centerY = y + 8f;
        // 应用旋转
        guiGraphics.pose().rotateAround(new Quaternionf().fromAxisAngleDeg(0, 0, 1, angle), centerX, centerY, 0);
        guiGraphics.pose().last().pose().scaleAround(scale, centerX, centerY, 0);
        guiGraphics.renderItem(item, x, y);
        guiGraphics.renderItemDecorations(minecraft.font, item, x, y, text);
        guiGraphics.pose().popPose();
    }

    public static void renderItem(GuiGraphics guiGraphics, ItemStack item, int x, int y, float scale, float angle) {
        renderItem(guiGraphics, item, x, y, scale, angle, "");
    }

    //text
    public static void drawStringAlign(GuiGraphics guiGraphics, String text, int startX, int startY, int lineWidth, AbstractComponent.AlignX alignX, int color, boolean lineBreak) {
        var withRuby = new StringWithRuby(text);
        if (lineBreak) withRuby.drawLineBreak(guiGraphics, startX, startY, lineWidth, alignX, color);
        else drawStringAlign(guiGraphics, Component.nullToEmpty(withRuby.noRuby), startX, startY, lineWidth, alignX, color, withRuby.rubyPartArrays());
    }

    public static void drawStringAlign(GuiGraphics guiGraphics, FormattedText text, int startX, int startY, int lineWidth, AbstractComponent.AlignX alignX, int color, RubyPart... rubyParts) {
        var font = minecraft.font;
        int renderX = startX; int renderY = startY;
        switch (alignX) {
            case CENTER -> renderX += (lineWidth - font.width(text)) / 2;
            case RIGHT ->  renderX +=  lineWidth - font.width(text);
        }
        if (rubyParts.length > 0) {
            String string = text.getString();
            for (var part : rubyParts) {
                int rubyStart = part.index();
                if (rubyStart < 0 || rubyStart + part.chars() > string.length()) continue;
                int rubyX = renderX + font.width(Component.nullToEmpty(string.substring(0, rubyStart)).getVisualOrderText());
                int subTextWidth = font.width(Component.nullToEmpty(string.substring(rubyStart, rubyStart + part.chars())).getVisualOrderText());
                Component rubyComponent = Component.nullToEmpty(part.ruby());
                int scaleX = rubyX + subTextWidth / 2;
                rubyX += (subTextWidth - font.width(rubyComponent.getVisualOrderText())) / 2;
                var pose = guiGraphics.pose();
                pose.pushPose();
                pose.last().pose().scaleAround(0.7f, scaleX, renderY + 5, 0);
                guiGraphics.drawString(font, rubyComponent, rubyX, renderY - 1, color, false);
                pose.popPose();
            }
            renderY += 6;
        }
        guiGraphics.drawString(font, Language.getInstance().getVisualOrder(text), renderX, renderY, color, false);
    }

    public record RubyPart(int index, int chars, String ruby) {}

    static class StringWithRuby {
        static final Pattern RUBY_PATTERN = Pattern.compile("<ruby\\s+(\\d+)\\s+([^>]*)>");
        final String raw;
        final List<RubyPart> rubyParts = new ArrayList<>();
        final boolean hasRuby;
        final String noRuby;

        public StringWithRuby(String raw) {
            this.raw = raw;
            if (raw.contains("<ruby ")) {
                StringBuilder noRubyBuilder = new StringBuilder();
                Matcher matcher = RUBY_PATTERN.matcher(raw);
                int lastEnd = 0;
                while (matcher.find()) {
                    int num = Integer.parseInt(matcher.group(1));
                    int end = matcher.end();
                    // 添加标签前的文本
                    noRubyBuilder.append(raw, lastEnd, matcher.start());
                    // 检查是否有足够的字符用于ruby标注
                    if (end + num > raw.length()) {lastEnd = end; break;}
                    String ruby = matcher.group(2);
                    rubyParts.add(new RubyPart(noRubyBuilder.length(), num, ruby));
                    lastEnd = end;
                }
                // 添加剩余的文本
                if (lastEnd < raw.length()) noRubyBuilder.append(raw.substring(lastEnd));
                this.hasRuby = !rubyParts.isEmpty();
                this.noRuby = noRubyBuilder.toString();
            } else {
                this.hasRuby = false;
                this.noRuby = raw;
            }
        }

        public RubyPart[] rubyPartArrays() {return rubyParts.toArray(new RubyPart[0]);}

        /**
         * 获取已移除标签文本{@link #noRuby}指定范围内的ruby标签，用于换行时绘制
         * @param start 开始索引（包含）
         * @param end 结束索引（不包含）
         */
        public RubyPart[] rubyFromTo(int start, int end) {
            if (!hasRuby) return new RubyPart[0];
            return rubyParts.stream().filter(part -> part.index >= start && part.index < end).map(p -> new RubyPart(p.index - start, p.chars, p.ruby)).toArray(RubyPart[]::new);
        }

        public void drawLineBreak(GuiGraphics guiGraphics, int startX, int startY, int lineWidth, AbstractComponent.AlignX alignX, int color) {
            var font = minecraft.font;
            int renderY = startY;
            int partStart = 0;
            for (var part : font.getSplitter().splitLines(Component.nullToEmpty(noRuby), lineWidth, Style.EMPTY)) {
                String textPart = part.getString();
                // 换行符在第一个字符时，跳过
                if (!noRuby.isEmpty() && noRuby.charAt(partStart) == '\n') partStart++;
                RubyPart[] rubyFromTo = rubyFromTo(partStart, partStart + textPart.length());
                drawStringAlign(guiGraphics, part, startX, renderY, lineWidth, alignX, color, rubyFromTo);
                if (rubyFromTo.length > 0) renderY += 6;
                renderY += font.lineHeight;
                partStart += textPart.length();
            }
        }
    }

    public static String translated(String key) {return Language.getInstance().getOrDefault(key);}

    //cursor

    public static void setCursor(int x, int y) {
        Window window = minecraft.getWindow();
        int w1 = window.getWidth();
        int w2 = screenWidth();
        int h1 = window.getHeight();
        int h2 = screenHeight();
        double ratW = (double) w2 / (double) w1;
        double ratH = (double) h2 / (double) h1;
        GLFW.glfwSetCursorPos(window.getWindow(), x / ratW, y / ratH);
    }

    public static Point getCursor() {
        Window window = minecraft.getWindow();
        int w1 = window.getWidth();
        int w2 = screenWidth();
        int h1 = window.getHeight();
        int h2 = screenHeight();
        double rW = (double) w2 / (double) w1;
        double rH = (double) h2 / (double) h1;
        return new Point((int) (rW * minecraft.mouseHandler.xpos()), (int) (rH * minecraft.mouseHandler.ypos()));
    }

    //util

    public static void renderOpacity(GuiGraphics guiGraphics, float opacity, Runnable runnable) {
        RenderSystem.enableBlend();
        guiGraphics.setColor(1f, 1f, 1f, opacity);
        runnable.run();
        RenderSystem.disableBlend();
    }

    //private

    private static void drawBuffer(BufferBuilder buf) {
        BufferUploader.drawWithShader(buf.end());
    }

    public static void beginRendering() {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    public static void finishRendering() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }

    private static BufferBuilder getBuffer() {
        return Tesselator.getInstance().getBuilder();
    }

    private static GameProfile createProfileComponent(String input) {
        try {
            return new GameProfile(UUID.fromString(input), null);
        } catch (IllegalArgumentException e) {
            return new GameProfile(null, input);
        }
    }

    private static final Map<String, ResourceLocation> skins = new HashMap<>();

    private static ResourceLocation getSkin(String input) {
        if (skins.containsKey(input)) return skins.get(input);
        // 尝试获取皮肤，并缓存到map中
        GameProfile profile = createProfileComponent(input);
        SkullBlockEntity.updateGameprofile(profile, gameProfile -> {
            SkinManager manager = minecraft.getSkinManager();
            var map = manager.getInsecureSkinInformation(gameProfile);
            if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) skins.put(input, manager.registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN));
        });
        if (skins.containsKey(input)) return skins.get(input);
        return DefaultPlayerSkin.getDefaultSkin(Objects.requireNonNull(minecraft.getUser().getProfileId()));
    }
}
