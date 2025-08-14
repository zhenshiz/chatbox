package com.zhenshiz.chatbox.utils.chatbox;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record FloatBlitRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x0, float y0,
        float x1, float y1,
        float u0, float u1,
        float v0, float v1,
        int color,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public FloatBlitRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            float x0, float y0,
            float x1, float y1,
            float u0, float u1,
            float v0, float v1,
            int color,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x0, y0, x1, y1, u0, u1, v0, v1, color, scissorArea, getBounds(x0, y0, x1, y1, pose, scissorArea));
    }

    public FloatBlitRenderState(
            GuiGraphics guiGraphics,
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            float x, float y,
            float width, float height,
            float u, float v,
            int color
    ) {
        this(pipeline, textureSetup, guiGraphics.pose(), x, y, x + width, y + height, 0, u, 0, v, color, guiGraphics.scissorStack.peek());
    }

    @Override
    public void buildVertices(VertexConsumer consumer, float z) {
        consumer.addVertexWith2DPose(pose, x0, y0, z).setUv(u0, v0).setColor(color);
        consumer.addVertexWith2DPose(pose, x0, y1, z).setUv(u0, v1).setColor(color);
        consumer.addVertexWith2DPose(pose, x1, y1, z).setUv(u1, v1).setColor(color);
        consumer.addVertexWith2DPose(pose, x1, y0, z).setUv(u1, v0).setColor(color);
    }

    @Nullable
    private static ScreenRectangle getBounds(float x0, float y0, float x1, float y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle screenRectangle = new ScreenRectangle((int) x0, (int) y0, (int) (x1 - x0), (int) (y1 - y0)).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(screenRectangle) : screenRectangle;
    }
}
