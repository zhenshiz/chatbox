package com.zhenshiz.chatbox.utils.chatbox;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
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

    public static final RenderPipeline QUADS = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation("pipeline/global_fill_pipeline")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build();

    private static void submitSimpleGuiElement(GuiGraphics guiGraphics, Consumer<VertexConsumer> builder, @Nullable ScreenRectangle bounds) {
        guiGraphics.guiRenderState.submitGuiElement(new GuiElementRenderState() {
            public void buildVertices(@NotNull VertexConsumer consumer) {builder.accept(consumer);}
            public @NotNull RenderPipeline pipeline() {return QUADS;}
            public @NotNull TextureSetup textureSetup() {return TextureSetup.noTexture();}
            public @Nullable ScreenRectangle scissorArea() {return guiGraphics.scissorStack.peek();}
            public @Nullable ScreenRectangle bounds() {return bounds;}
        });
    }

    //fill

    //矩形
    public static void fillRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        guiGraphics.fill(x, y, x + w, y + h, color);
    }

    //todo 凡是没有使用的方法，都是没有修好的，勿用（真用到了再修）
    //圆弧
    public static void fillArc(GuiGraphics guiGraphics, int cX, int cY, int radius, int start, int end, int color) {
        submitSimpleGuiElement(guiGraphics, consumer -> {
            Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose());
            consumer.addVertexWith2DPose(pose, (float) cX, (float) cY).setColor(color);

            for (int i = start - 90; i <= end - 90; i++) {
                double angle = Math.toRadians(i);
                float x = (float) (Math.cos(angle) * radius) + cX;
                float y = (float) (Math.sin(angle) * radius) + cY;
                consumer.addVertexWith2DPose(pose, x, y).setColor(color);
            }
        }, new ScreenRectangle(cX - radius, cY - radius, 2 * radius, 2 * radius));
    }

    //圆
    public static void fillCircle(GuiGraphics guiGraphics, int cX, int cY, int radius, int color) {
        fillArc(guiGraphics, cX, cY, radius, 0, 360, color);
    }

    //环形扇区
    public static void fillAnnulusArc(GuiGraphics guiGraphics, int cx, int cy, int radius, int start, int end, int thickness, int color) {
        submitSimpleGuiElement(guiGraphics, consumer -> {
            Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose());
            for (int i = start - 90; i <= end - 90; i++) {
                float angle = (float) Math.toRadians(i);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                float x1 = cx + cos * radius;
                float y1 = cy + sin * radius;
                float x2 = cx + cos * (radius + thickness);
                float y2 = cy + sin * (radius + thickness);
                consumer.addVertexWith2DPose(pose, x1, y1).setColor(color);
                consumer.addVertexWith2DPose(pose, x2, y2).setColor(color);
            }
        }, new ScreenRectangle(cx - radius, cy - radius, 2 * radius, 2 * radius));
    }

    //环形圆
    public static void fillAnnulus(GuiGraphics guiGraphics, int cx, int cy, int radius, int thickness, int color) {
        fillAnnulusArc(guiGraphics, cx, cy, radius, 0, 360, thickness, color);
    }

    //实心圆角矩形
    public static void fillRoundRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);
        int finalR = r;
        submitSimpleGuiElement(guiGraphics, consumer -> {
            Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose());

            consumer.addVertexWith2DPose(pose, x + w / 2F, y + h / 2F).setColor(color);

            int[][] corners = {
                    {x + w - finalR, y + finalR},
                    {x + w - finalR, y + h - finalR},
                    {x + finalR, y + h - finalR},
                    {x + finalR, y + finalR}
            };

            for (int corner = 0; corner < 4; corner++) {
                int cornerStart = (corner - 1) * 90;
                int cornerEnd = cornerStart + 90;
                for (int i = cornerStart; i <= cornerEnd; i += 10) {
                    float angle = (float) Math.toRadians(i);
                    float rx = corners[corner][0] + (float) (Math.cos(angle) * finalR);
                    float ry = corners[corner][1] + (float) (Math.sin(angle) * finalR);
                    consumer.addVertexWith2DPose(pose, rx, ry).setColor(color);
                }
            }

            consumer.addVertexWith2DPose(pose, corners[0][0], y).setColor(color);
        }, new ScreenRectangle(x, y, w, h));
    }

    //圆角阴影边框
    public static void fillRoundShadow(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int thickness, int innerColor, int outerColor) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);
        int finalR = r;
        submitSimpleGuiElement(guiGraphics, consumer -> {
            Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose());

            int[][] corners = {
                    {x + w - finalR, y + finalR},
                    {x + w - finalR, y + h - finalR},
                    {x + finalR, y + h - finalR},
                    {x + finalR, y + finalR}
            };

            for (int corner = 0; corner < 4; corner++) {
                int cornerStart = (corner - 1) * 90;
                int cornerEnd = cornerStart + 90;
                for (int i = cornerStart; i <= cornerEnd; i += 10) {
                    float angle = (float) Math.toRadians(i);
                    float rx1 = corners[corner][0] + (float) (Math.cos(angle) * finalR);
                    float ry1 = corners[corner][1] + (float) (Math.sin(angle) * finalR);
                    float rx2 = corners[corner][0] + (float) (Math.cos(angle) * (finalR + thickness));
                    float ry2 = corners[corner][1] + (float) (Math.sin(angle) * (finalR + thickness));
                    consumer.addVertexWith2DPose(pose, rx1, ry1).setColor(innerColor);
                    consumer.addVertexWith2DPose(pose, rx2, ry2).setColor(outerColor);
                }
            }

            consumer.addVertexWith2DPose(pose, corners[0][0], y).setColor(innerColor);
            consumer.addVertexWith2DPose(pose, corners[0][0], y - thickness).setColor(outerColor);
        }, new ScreenRectangle(x, y, w, h));
    }

    //上圆角矩形
    public static void fillRoundTabTop(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);
        int finalR = r;
        submitSimpleGuiElement(guiGraphics, consumer -> {
            Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose());

            consumer.addVertexWith2DPose(pose, x + w / 2F, y + h / 2F).setColor(color);

            int[][] corners = {
                    {x + finalR, y + finalR},
                    {x + w - finalR, y + finalR}
            };

            for (int corner = 0; corner < 2; corner++) {
                int cornerStart = (corner - 2) * 90;
                int cornerEnd = cornerStart + 90;
                for (int i = cornerStart; i <= cornerEnd; i += 10) {
                    float angle = (float) Math.toRadians(i);
                    float rx = corners[corner][0] + (float) (Math.cos(angle) * finalR);
                    float ry = corners[corner][1] + (float) (Math.sin(angle) * finalR);
                    consumer.addVertexWith2DPose(pose, rx, ry).setColor(color);
                }
            }

            consumer.addVertexWith2DPose(pose, x + w, y + h).setColor(color);
            consumer.addVertexWith2DPose(pose, x, y + h).setColor(color);
            consumer.addVertexWith2DPose(pose, x, corners[0][1]).setColor(color); // connect last to first vertex
        }, new ScreenRectangle(x, y, w, h));
    }

    //下圆角矩形
    public static void fillRoundTabBottom(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);
        int finalR = r;
        submitSimpleGuiElement(guiGraphics, consumer -> {
            Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose());

            consumer.addVertexWith2DPose(pose, x + w / 2F, y + h / 2F).setColor(color);

            int[][] corners = {
                    {x + w - finalR, y + h - finalR},
                    {x + finalR, y + h - finalR}
            };

            for (int corner = 0; corner < 2; corner++) {
                int cornerStart = corner * 90;
                int cornerEnd = cornerStart + 90;
                for (int i = cornerStart; i <= cornerEnd; i += 10) {
                    float angle = (float) Math.toRadians(i);
                    float rx = corners[corner][0] + (float) (Math.cos(angle) * finalR);
                    float ry = corners[corner][1] + (float) (Math.sin(angle) * finalR);
                    consumer.addVertexWith2DPose(pose, rx, ry).setColor(color);
                }
            }

            consumer.addVertexWith2DPose(pose, x, y).setColor(color);
            consumer.addVertexWith2DPose(pose, x + w, y).setColor(color);
            consumer.addVertexWith2DPose(pose, x + w, corners[0][1]).setColor(color); // connect last to first vertex
        }, new ScreenRectangle(x, y, w, h));
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
        drawLine(guiGraphics, x1, y1, x2, y2, 0.5F, color);
    }

    public static void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float angle = (float) Math.atan2(dy, dx);
        float t = thickness / 2.0F;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose()).rotateAbout(angle, x1, y1);
        submitSimpleGuiElement(guiGraphics, consumer -> {
            consumer.addVertexWith2DPose(pose, x1 - t, y1 - t           ).setColor(color);
            consumer.addVertexWith2DPose(pose, x1 + length + t, y1 - t  ).setColor(color);
            consumer.addVertexWith2DPose(pose, x1 + length + t, y1 + t  ).setColor(color);
            consumer.addVertexWith2DPose(pose, x1 - t, y1 + t           ).setColor(color);
        }, ScreenRectangle.empty());
    }

    //扇形
    public static void drawArc(GuiGraphics guiGraphics, int cX, int cY, int radius, int start, int end, int color) {
        submitSimpleGuiElement(guiGraphics, consumer -> {
            Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose());
            for (int i = start - 90; i <= end - 90; i++) {
                double angle = Math.toRadians(i);
                float x = (float) (Math.cos(angle) * radius) + cX;
                float y = (float) (Math.sin(angle) * radius) + cY;
                consumer.addVertexWith2DPose(pose, x, y).setColor(color);
            }
        }, new ScreenRectangle(cX - radius, cY - radius, 2 * radius, 2 * radius));
    }

    //圆
    public static void drawCircle(GuiGraphics guiGraphics, int cX, int cY, int radius, int color) {
        drawArc(guiGraphics, cX, cY, radius, 0, 360, color);
    }

    //圆角矩形
    public static void drawRoundRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);
        int finalR = r;
        submitSimpleGuiElement(guiGraphics, consumer -> {
            Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose());

            int[][] corners = {
                    {x + w - finalR, y + finalR},
                    {x + w - finalR, y + h - finalR},
                    {x + finalR, y + h - finalR},
                    {x + finalR, y + finalR}
            };

            for (int corner = 0; corner < 4; corner++) {
                int cornerStart = (corner - 1) * 90;
                int cornerEnd = cornerStart + 90;
                for (int i = cornerStart; i <= cornerEnd; i += 10) {
                    float angle = (float) Math.toRadians(i);
                    float rx = corners[corner][0] + (float) (Math.cos(angle) * finalR);
                    float ry = corners[corner][1] + (float) (Math.sin(angle) * finalR);
                    consumer.addVertexWith2DPose(pose, rx, ry).setColor(color);
                }
            }

            consumer.addVertexWith2DPose(pose, corners[0][0], y).setColor(color); // connect last to first vertex
        }, new ScreenRectangle(x, y, w, h));
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
    public static void renderImage(GuiGraphics guiGraphics, Matrix3x2f pose, Identifier identifier, float x, float y, float uw, float uh, float width, float height, float opacity) {
        AbstractTexture texture = minecraft.getTextureManager().getTexture(identifier);
        guiGraphics.guiRenderState.submitGuiElement(new FloatBlitRenderState(guiGraphics, RenderPipelines.GUI_TEXTURED, TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()), pose, x, y, width, height, uw, uh, getColor(opacity)));
    }

    public static void renderImage(GuiGraphics guiGraphics, Identifier identifier, float x, float y, float width, float height, float scale, float opacity, float angle, List<ChatBoxTheme.Portrait.Attachment> attachments) {
        Matrix3x2f pose = new Matrix3x2f(guiGraphics.pose()).rotateAbout((float) Math.toRadians(angle), x + width / 2, y + height / 2).scaleAround(scale, x + width / 2, y + height / 2);
        renderImage(guiGraphics, pose, identifier, x, y, 1, 1, width, height, opacity);
        for (var attachment : attachments) {
            var a = attachment.mapParameter();
            renderImage(guiGraphics, pose, Identifier.parse(a.value), x + a.x, y + a.y, 1, 1, a.width, a.height, opacity);
        }
    }

    public static void renderImage(GuiGraphics guiGraphics, Identifier identifier, float x, float y, float width, float height, float scale, float opacity, float angle) {
        renderImage(guiGraphics, identifier, x, y, width, height, scale, opacity, angle, List.of());
    }

    public static void renderPlayerHead(GuiGraphics guiGraphics, String input, int x, int y, int size, float scale, float opacity, float angle) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().rotateAbout((float) Math.toRadians(angle), x + (float) size / 2, y + (float) size / 2).scaleAround(scale, x + (float) size / 2, y + (float) size / 2);
        var skin = getSkin(input).body().texturePath();
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8, 8, size, size, 8, 8, 64, 64, getColor(opacity));
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, skin, x - 1, y - 1, 40, 8, size + 2, size + 2, 8, 8, 64, 64, getColor(opacity));
        guiGraphics.pose().popMatrix();
    }

    public static void renderItem(GuiGraphics guiGraphics, ItemStack item, int x, int y, float scale, float angle, String text) {
        guiGraphics.pose().pushMatrix();
        // 应用旋转
        guiGraphics.pose().rotateAbout((float) Math.toRadians(angle), x + 8f, y + 8f).scaleAround(scale, x + 8f, y + 8f);
        guiGraphics.renderItem(item, x, y);
        guiGraphics.renderItemDecorations(minecraft.font, item, x, y, text);
        guiGraphics.pose().popMatrix();
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
                pose.pushMatrix();
                pose.scaleAround(0.7f, 0.7f, scaleX, renderY + 5);
                guiGraphics.drawString(font, rubyComponent, rubyX, renderY - 1, color, false);
                pose.popMatrix();
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
                if (noRuby.length() > partStart && noRuby.charAt(partStart) == '\n') partStart++;
                RubyPart[] rubyFromTo = rubyFromTo(partStart, partStart + textPart.length());
                drawStringAlign(guiGraphics, part, startX, renderY, lineWidth, alignX, color, rubyFromTo);
                if (rubyFromTo.length > 0) renderY += 6;
                renderY += font.lineHeight;
                partStart += textPart.length();
            }
        }
    }

    public static String translated(String key) {return Language.getInstance().getOrDefault(key);}

    public static void renderTooltip(GuiGraphics guiGraphics, Component tooltip, int x, int y) {
        guiGraphics.renderTooltip(minecraft.font, List.of(new ClientTextTooltip(tooltip.getVisualOrderText())), x, y, DefaultTooltipPositioner.INSTANCE, null);
    }

    //cursor

    public static void setCursor(int x, int y) {
        Window window = minecraft.getWindow();
        int w1 = window.getWidth();
        int w2 = screenWidth();
        int h1 = window.getHeight();
        int h2 = screenHeight();
        double ratW = (double) w2 / (double) w1;
        double ratH = (double) h2 / (double) h1;
        GLFW.glfwSetCursorPos(window.handle(), x / ratW, y / ratH);
    }

    public static Vec2 getCursor() {
        Window window = minecraft.getWindow();
        int w1 = window.getWidth();
        int w2 = screenWidth();
        int h1 = window.getHeight();
        int h2 = screenHeight();
        double rW = (double) w2 / (double) w1;
        double rH = (double) h2 / (double) h1;
        return new Vec2((int) (rW * minecraft.mouseHandler.xpos()), (int) (rH * minecraft.mouseHandler.ypos()));
    }

    //util

    public static int getColor(float opacity) {
        return ARGB.color((int) (opacity / 100 * 255), 255, 255, 255);
    }

    //private

    private static final Map<String, PlayerSkin> skins = new HashMap<>();

    private static void handleGameProfileAsync(String input) {
        ResolvableProfile component = createProfileComponent(input);
        component.resolveProfile(minecraft.services().profileResolver())
                .thenApplyAsync(profile -> {
                    try {
                        minecraft.getSkinManager().get(profile).get()
                                .ifPresent(playerSkin -> skins.put(input, playerSkin));
                    } catch (InterruptedException | ExecutionException ignored) {}
                    return profile;
                })
                .exceptionally(ex -> null);
    }

    private static ResolvableProfile createProfileComponent(String input) {
        try {
            UUID uuid = UUID.fromString(input);
            return ResolvableProfile.createUnresolved(uuid);
        } catch (IllegalArgumentException e) {
            return ResolvableProfile.createUnresolved(input);
        }
    }

    private static PlayerSkin getSkin(String input) {
        if (skins.containsKey(input)) return skins.get(input);
        handleGameProfileAsync(input);
        if (skins.containsKey(input)) return skins.get(input);
        return DefaultPlayerSkin.get(minecraft.getUser().getProfileId());
    }
}
