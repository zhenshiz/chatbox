package com.zhenshiz.chatbox.editor;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.stb.STBImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Base64;
import java.util.Objects;

public class ImageExport {
    public static RenderTarget renderTarget = new TextureTarget("render_item", 64, 64, true);
    private static final ProjectionMatrixBuffer projBuffer = new ProjectionMatrixBuffer("ItemStack Proj");
    private static final ItemStackRenderState itemStackRenderState = new ItemStackRenderState();

    public static JsonObject renderItem(Identifier id) throws Exception {
        if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalArgumentException("Unknown item: " + id);
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
        renderItemStack(stack, renderTarget, 1);
        try (NativeImage image = grab(renderTarget)) {
            JsonObject result = new JsonObject();
            var url = "data:image/png;base64," + Base64.getEncoder().encodeToString(asByteArray(image));
            result.addProperty("url", url);
            result.addProperty("width", 64);
            result.addProperty("height", 64);
            return result;
        }
    }

    public static void renderItemStack(ItemStack itemStack, RenderTarget fbo, float scaleMul) {
        var mc = Minecraft.getInstance();
        var width = fbo.width;
        var height = fbo.height;

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projBuffer.getBuffer(new Matrix4f().setOrtho(0, width, height, 0, -4000.0F, 4000.0F)), ProjectionType.ORTHOGRAPHIC);
        var bufferSource = mc.renderBuffers().bufferSource();
        var submitNodeCollector = mc.gameRenderer.getSubmitNodeStorage();
        var featureDispatcher = mc.gameRenderer.getFeatureRenderDispatcher();

        float scale = Math.min(width, height);
        var shift = Math.abs(width - height) / 2f;
        var shiftX = width > height ? shift : 0;
        var shiftY = height > width ? shift : 0;
        scale *= scaleMul;
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(new Matrix4f());

        var poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.translate(shiftX, shiftY, 0);
        poseStack.translate((float) width / 2, (float) height / 2, 0);
        poseStack.scale(scale, -scale, scale);
        mc.getItemModelResolver()
                .updateForTopItem(itemStackRenderState, itemStack, ItemDisplayContext.GUI, null, null, 42);
        var flag = !itemStackRenderState.usesBlockLight();
        if (flag) {
            bufferSource.endBatch();
            mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        }
        clear(fbo, true);
        RenderSystem.outputColorTextureOverride = fbo.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = fbo.getDepthTextureView();
        itemStackRenderState.submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
        featureDispatcher.renderAllFeatures();
        bufferSource.endBatch();
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        if (flag) mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        poseStack.popPose();
        modelView.popMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    public static CommandEncoder getCommandEncoder() {
        return RenderSystem.getDevice().createCommandEncoder();
    }

    public static void clear(RenderTarget target, boolean clearDepth) {
        if (target.useDepth && clearDepth) {
            getCommandEncoder().clearColorAndDepthTextures(Objects.requireNonNull(target.getColorTexture()), 0, Objects.requireNonNull(target.getDepthTexture()), 1d);
        } else {
            getCommandEncoder().clearColorTexture(Objects.requireNonNull(target.getColorTexture()), 0);
        }
    }

    public static byte[] asByteArray(NativeImage image) throws IOException {
        byte[] bytes;
        try (
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                WritableByteChannel channel = Channels.newChannel(stream)
        ) {
            if (!image.writeToChannel(channel)) {
                throw new IOException("Could not write image to byte array: " + STBImage.stbi_failure_reason());
            }
            bytes = stream.toByteArray();
        }
        return bytes;
    }

    public static NativeImage grab(RenderTarget target) {
        var sourceTexture = target.getColorTexture();
        if (sourceTexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        }
        int width = target.width;
        int height = target.height;
        int pixelSize = sourceTexture.getFormat().pixelSize();
        try (var buffer = RenderSystem.getDevice().createBuffer(() -> "Sync screenshot buffer", 9, (long) width * (long) height * (long) pixelSize)) {
            var encoder = getCommandEncoder();
            // 提交纹理→buffer 的拷贝命令；回调置空，我们不依赖它
            encoder.copyTextureToBuffer(sourceTexture, buffer, 0L, () -> {}, 0);
            // 立刻 map：mapBuffer 对 read 操作会同步等待 GPU 完成拷贝
            try (var read = encoder.mapBuffer(buffer, true, false)) {
                NativeImage image = new NativeImage(width, height, false);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int argb = read.data().getInt((x + y * width) * pixelSize);
                        image.setPixelABGR(x, height - y - 1, argb | 0xFF000000);
                    }
                }
                return image;
            }
        }
    }
}
