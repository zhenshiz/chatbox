package com.zhenshiz.chatbox.utils.chatbox;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
        guiGraphics.fill(x, y, x + w, y + h, color);
    }

    //圆弧
    public static void fillArc(GuiGraphics guiGraphics, int cX, int cY, int radius, int start, int end, int color) {
        guiGraphics.guiRenderState.submitGuiElement(new GuiElementRenderState() {
            public void buildVertices(VertexConsumer consumer, float z) {
                Matrix3x2fStack pose = guiGraphics.pose();
                consumer.addVertexWith2DPose(pose, (float) cX, (float) cY, z).setColor(color);

                for (int i = start - 90; i <= end - 90; i++) {
                    double angle = Math.toRadians(i);
                    float x = (float) (Math.cos(angle) * radius) + cX;
                    float y = (float) (Math.sin(angle) * radius) + cY;
                    consumer.addVertexWith2DPose(pose, x, y, z).setColor(color);
                }
            }
            public @NotNull RenderPipeline pipeline() {return RenderPipelines.GUI;}
            public @NotNull TextureSetup textureSetup() {return TextureSetup.noTexture();}
            public @Nullable ScreenRectangle scissorArea() {return guiGraphics.scissorStack.peek();}
            public ScreenRectangle bounds() {
                return new ScreenRectangle(cX - radius, cY - radius, 2 * radius, 2 * radius);
            }
        });
    }

    //圆
    public static void fillCircle(GuiGraphics guiGraphics, int cX, int cY, int radius, int color) {
        fillArc(guiGraphics, cX, cY, radius, 0, 360, color);
    }

    //环形扇区
    public static void fillAnnulusArc(GuiGraphics guiGraphics, int cx, int cy, int radius, int start, int end, int thickness, int color) {
        guiGraphics.guiRenderState.submitGuiElement(new GuiElementRenderState() {
            public void buildVertices(VertexConsumer consumer, float z) {
                Matrix3x2fStack pose = guiGraphics.pose();
                for (int i = start - 90; i <= end - 90; i++) {
                    float angle = (float) Math.toRadians(i);
                    float cos = (float) Math.cos(angle);
                    float sin = (float) Math.sin(angle);
                    float x1 = cx + cos * radius;
                    float y1 = cy + sin * radius;
                    float x2 = cx + cos * (radius + thickness);
                    float y2 = cy + sin * (radius + thickness);
                    consumer.addVertexWith2DPose(pose, x1, y1, z).setColor(color);
                    consumer.addVertexWith2DPose(pose, x2, y2, z).setColor(color);
                }
            }
            public @NotNull RenderPipeline pipeline() {return RenderPipelines.GUI;}
            public @NotNull TextureSetup textureSetup() {return TextureSetup.noTexture();}
            public @Nullable ScreenRectangle scissorArea() {return guiGraphics.scissorStack.peek();}
            public ScreenRectangle bounds() {
                return new ScreenRectangle(cx - radius, cy - radius, 2 * radius, 2 * radius);
            }
        });
    }

    //环形圆
    public static void fillAnnulus(GuiGraphics guiGraphics, int cx, int cy, int radius, int thickness, int color) {
        fillAnnulusArc(guiGraphics, cx, cy, radius, 0, 360, thickness, color);
    }

    //实心圆角矩形
    public static void fillRoundRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);
        int finalR = r;
        guiGraphics.guiRenderState.submitGuiElement(new GuiElementRenderState() {
            public void buildVertices(VertexConsumer consumer, float z) {
                Matrix3x2fStack pose = guiGraphics.pose();

                consumer.addVertexWith2DPose(pose, x + w / 2F, y + h / 2F, 0).setColor(color);

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
                        consumer.addVertexWith2DPose(pose, rx, ry, 0).setColor(color);
                    }
                }

                consumer.addVertexWith2DPose(pose, corners[0][0], y, 0).setColor(color);
            }
            public @NotNull RenderPipeline pipeline() {return RenderPipelines.GUI;}
            public @NotNull TextureSetup textureSetup() {return TextureSetup.noTexture();}
            public @Nullable ScreenRectangle scissorArea() {return guiGraphics.scissorStack.peek();}
            public ScreenRectangle bounds() {return new ScreenRectangle(x, y, w, h);}
        });
    }

    //圆角阴影边框
    public static void fillRoundShadow(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int thickness, int innerColor, int outerColor) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);
        int finalR = r;
        guiGraphics.guiRenderState.submitGuiElement(new GuiElementRenderState() {
            public void buildVertices(VertexConsumer consumer, float z) {
                Matrix3x2fStack pose = guiGraphics.pose();

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
                        consumer.addVertexWith2DPose(pose, rx1, ry1, 0).setColor(innerColor);
                        consumer.addVertexWith2DPose(pose, rx2, ry2, 0).setColor(outerColor);
                    }
                }

                consumer.addVertexWith2DPose(pose, corners[0][0], y, 0).setColor(innerColor);
                consumer.addVertexWith2DPose(pose, corners[0][0], y - thickness, 0).setColor(outerColor);
            }
            public @NotNull RenderPipeline pipeline() {return RenderPipelines.GUI;}
            public @NotNull TextureSetup textureSetup() {return TextureSetup.noTexture();}
            public @Nullable ScreenRectangle scissorArea() {return guiGraphics.scissorStack.peek();}
            public ScreenRectangle bounds() {return new ScreenRectangle(x, y, w, h);}
        });
    }

    //上圆角矩形
    public static void fillRoundTabTop(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);
        int finalR = r;
        guiGraphics.guiRenderState.submitGuiElement(new GuiElementRenderState() {
            public void buildVertices(VertexConsumer consumer, float z) {
                Matrix3x2fStack pose = guiGraphics.pose();

                consumer.addVertexWith2DPose(pose, x + w / 2F, y + h / 2F, 0).setColor(color);

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
                        consumer.addVertexWith2DPose(pose, rx, ry, 0).setColor(color);
                    }
                }

                consumer.addVertexWith2DPose(pose, x + w, y + h, 0).setColor(color);
                consumer.addVertexWith2DPose(pose, x, y + h, 0).setColor(color);
                consumer.addVertexWith2DPose(pose, x, corners[0][1], 0).setColor(color); // connect last to first vertex
            }
            public @NotNull RenderPipeline pipeline() {return RenderPipelines.GUI;}
            public @NotNull TextureSetup textureSetup() {return TextureSetup.noTexture();}
            public @Nullable ScreenRectangle scissorArea() {return guiGraphics.scissorStack.peek();}
            public ScreenRectangle bounds() {return new ScreenRectangle(x, y, w, h);}
        });
    }

    //下圆角矩形
    public static void fillRoundTabBottom(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);
        int finalR = r;
        guiGraphics.guiRenderState.submitGuiElement(new GuiElementRenderState() {
            public void buildVertices(VertexConsumer consumer, float z) {
                Matrix3x2fStack pose = guiGraphics.pose();

                consumer.addVertexWith2DPose(pose, x + w / 2F, y + h / 2F, 0).setColor(color);

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
                        consumer.addVertexWith2DPose(pose, rx, ry, 0).setColor(color);
                    }
                }

                consumer.addVertexWith2DPose(pose, x, y, 0).setColor(color);
                consumer.addVertexWith2DPose(pose, x + w, y, 0).setColor(color);
                consumer.addVertexWith2DPose(pose, x + w, corners[0][1], 0).setColor(color); // connect last to first vertex
            }
            public @NotNull RenderPipeline pipeline() {return RenderPipelines.GUI;}
            public @NotNull TextureSetup textureSetup() {return TextureSetup.noTexture();}
            public @Nullable ScreenRectangle scissorArea() {return guiGraphics.scissorStack.peek();}
            public ScreenRectangle bounds() {return new ScreenRectangle(x, y, w, h);}
        });
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
        guiGraphics.guiRenderState.submitGuiElement(new GuiElementRenderState() {
            public void buildVertices(VertexConsumer consumer, float z) {
                Matrix3x2fStack pose = guiGraphics.pose();
                consumer.addVertexWith2DPose(pose, (float) x1, (float) y1, 0).setColor(color);
                consumer.addVertexWith2DPose(pose, (float) x2, (float) y2, 0).setColor(color);
            }
            public @NotNull RenderPipeline pipeline() {return RenderPipelines.GUI;}
            public @NotNull TextureSetup textureSetup() {return TextureSetup.noTexture();}
            public @Nullable ScreenRectangle scissorArea() {return guiGraphics.scissorStack.peek();}
            public ScreenRectangle bounds() {
                return new ScreenRectangle(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
            }
        });
    }


    //扇形
    public static void drawArc(GuiGraphics guiGraphics, int cX, int cY, int radius, int start, int end, int color) {
        guiGraphics.guiRenderState.submitGuiElement(new GuiElementRenderState() {
            public void buildVertices(VertexConsumer consumer, float z) {
                Matrix3x2fStack pose = guiGraphics.pose();
                for (int i = start - 90; i <= end - 90; i++) {
                    double angle = Math.toRadians(i);
                    float x = (float) (Math.cos(angle) * radius) + cX;
                    float y = (float) (Math.sin(angle) * radius) + cY;
                    consumer.addVertexWith2DPose(pose, x, y, 0).setColor(color);
                }
            }
            public @NotNull RenderPipeline pipeline() {return RenderPipelines.GUI;}
            public @NotNull TextureSetup textureSetup() {return TextureSetup.noTexture();}
            public @Nullable ScreenRectangle scissorArea() {return guiGraphics.scissorStack.peek();}
            public ScreenRectangle bounds() {
                return new ScreenRectangle(cX - radius, cY - radius, 2 * radius, 2 * radius);
            }
        });
    }

    //圆
    public static void drawCircle(GuiGraphics guiGraphics, int cX, int cY, int radius, int color) {
        drawArc(guiGraphics, cX, cY, radius, 0, 360, color);
    }

    //圆角矩形
    public static void drawRoundRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int r, int color) {
        r = Mth.clamp(r, 0, Math.min(w, h) / 2);
        int finalR = r;
        guiGraphics.guiRenderState.submitGuiElement(new GuiElementRenderState() {
            public void buildVertices(VertexConsumer consumer, float z) {
                Matrix3x2fStack pose = guiGraphics.pose();

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
                        consumer.addVertexWith2DPose(pose, rx, ry, 0).setColor(color);
                    }
                }

                consumer.addVertexWith2DPose(pose, corners[0][0], y, 0).setColor(color); // connect last to first vertex
            }
            public @NotNull RenderPipeline pipeline() {return RenderPipelines.GUI;}
            public @NotNull TextureSetup textureSetup() {return TextureSetup.noTexture();}
            public @Nullable ScreenRectangle scissorArea() {return guiGraphics.scissorStack.peek();}
            public ScreenRectangle bounds() {return new ScreenRectangle(x, y, w, h);}
        });
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
    public static void renderImage(GuiGraphics guiGraphics, ResourceLocation resourceLocation, float x, float y, float z, float uw, float uh, float width, float height, float opacity) {
        GpuTextureView view = minecraft.getTextureManager().getTexture(resourceLocation).getTextureView();
        guiGraphics.guiRenderState.submitGuiElement(new FloatBlitRenderState(guiGraphics, RenderPipelines.GUI_TEXTURED, TextureSetup.singleTexture(view), x, y, width, height, uw, uh, getColor(opacity)));
    }

    public static void renderImage(GuiGraphics guiGraphics, ResourceLocation resourceLocation, float x, float y, float z, float width, float height, float scale, float opacity) {
        renderImage(guiGraphics, resourceLocation, x / scale, y / scale, z, 1, 1, width * scale, height * scale, opacity);
    }

    public static void renderPlayerHead(GuiGraphics guiGraphics, String input, int x, int y, int size, float scale, float opacity) {
        x = (int) (x / scale);
        y = (int) (y / scale);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(scale, scale);
        PlayerFaceRenderer.draw(guiGraphics, getSkin(input), x, y, size, getColor(opacity));
        guiGraphics.pose().popMatrix();
    }

    public static void renderItem(GuiGraphics guiGraphics, ItemStack item, int x, int y, float scale, String text) {
        x = (int) (x / scale);
        y = (int) (y / scale);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.renderItem(item, x, y);
        guiGraphics.renderItemDecorations(minecraft.font, item, x, y, text);
        guiGraphics.pose().popMatrix();
    }

    public static void renderItem(GuiGraphics guiGraphics, ItemStack item, int x, int y, float scale) {
        renderItem(guiGraphics, item, x, y, scale, "");
    }

    // text
    public static void drawLeftScaleText(GuiGraphics guiGraphics, Component component, int x, int y, float scale, boolean shadow, int color) {
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.scale(scale, scale);

        float rescale = 1 / scale;
        x = (int) (x * rescale);
        y = (int) (y * rescale);

        guiGraphics.drawString(minecraft.font, component, x, y, color, shadow);
        poseStack.scale(rescale, rescale);
    }

    public static void drawCenterScaleText(GuiGraphics guiGraphics, Component component, int centerX, int y, float scale, boolean shadow, int color) {
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.scale(scale, scale);

        float rescale = 1 / scale;
        centerX = (int) (centerX * rescale);
        centerX = centerX - (minecraft.font.width(component) / 2);
        y = (int) (y * rescale);

        guiGraphics.drawString(minecraft.font, component, centerX, y, color, shadow);
        poseStack.scale(rescale, rescale);
    }

    public static void drawRightScaleText(GuiGraphics guiGraphics, Component component, int rightX, int y, float scale, boolean shadow, int color) {
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.scale(scale, scale);

        float rescale = 1 / scale;
        rightX = (int) (rightX * rescale);
        rightX = rightX - minecraft.font.width(component);
        y = (int) (y * rescale);

        guiGraphics.drawString(minecraft.font, component, rightX, y, color, shadow);
        poseStack.scale(rescale, rescale);
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

    public static int getColor(float opacity) {
        return ARGB.color((int) (opacity / 100 * 255), 255, 255, 255);
    }

    //private

    private static final Map<String, PlayerSkin> skins = new HashMap<>();

    private static void handleGameProfileAsync(String input) {
        ResolvableProfile component = createProfileComponent(input);
        component.resolve()
                .thenApplyAsync(result -> {
                    GameProfile profile = result.gameProfile();
                    try {
                        minecraft.getSkinManager().getOrLoad(profile).get()
                                .ifPresent(playerSkin -> skins.put(input, playerSkin));
                    } catch (InterruptedException | ExecutionException ignored) {}
                    return profile;
                })
                .exceptionally(ex -> null);
    }

    private static ResolvableProfile createProfileComponent(String input) {
        try {
            UUID uuid = UUID.fromString(input);
            return new ResolvableProfile(Optional.empty(), Optional.of(uuid), new PropertyMap());
        } catch (IllegalArgumentException e) {
            return new ResolvableProfile(Optional.of(input), Optional.empty(), new PropertyMap());
        }
    }

    private static PlayerSkin getSkin(String input) {
        if (skins.containsKey(input)) return skins.get(input);
        handleGameProfileAsync(input);
        if (skins.containsKey(input)) return skins.get(input);
        return DefaultPlayerSkin.get(minecraft.getUser().getProfileId());
    }
}
