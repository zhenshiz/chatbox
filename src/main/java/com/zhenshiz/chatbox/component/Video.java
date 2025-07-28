package com.zhenshiz.chatbox.component;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec2;
import net.neoforged.fml.loading.FMLLoader;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.image.ImageRenderer;
import org.watermedia.api.player.videolan.VideoPlayer;
import org.watermedia.core.tools.JarTool;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Video extends AbstractComponent<Video> {
    private static final DateFormat FORMAT = new SimpleDateFormat("HH:mm:ss");

    static {
        FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-00:00"));
    }

    private float actualX, actualY, actualWidth, actualHeight;
    // STATUS
    private int tick = 0;
    private float fadeStep30 = 0;
    private float fadeStep10 = 0;
    private boolean started;
    private float volume;
    private final boolean loop;

    // CONTROL
    private final boolean canControl;
    public final boolean canSkip;

    // TOOLS
    private final Minecraft minecraft = Minecraft.getInstance();
    private final VideoPlayer player;

    // VIDEO INFO
    int videoTexture = -1;

    ImageRenderer IMG_PAUSED = ImageAPI.renderer(JarTool.readImage("/pictures/paused.png"), true);
    ImageRenderer IMG_STEP30 = ImageAPI.renderer(JarTool.readImage("/pictures/step30.png"), true);
    ImageRenderer IMG_STEP10 = ImageAPI.renderer(JarTool.readImage("/pictures/step10.png"), true);

    public Video(URI uri, boolean canControl, boolean canSkip, boolean loop) {
        //minecraft.getSoundManager().pause();
        this.volume = 100;
        this.canControl = canControl;
        this.canSkip = canSkip;
        this.loop = loop;

        this.player = new VideoPlayer(minecraft);
        ChatBox.LOGGER.info("Playing video ({}blocked) ({} with volume: {}", canControl ? "not " : "", uri, (int) (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * 100));

        player.setVolume((int) (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * 100));
        started = true;
        player.start(uri);
    }

    public boolean isPlaying() {
        return started;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        Vec2 pos = getCurrentPosition();
        actualX = getResponsiveWidth(pos.x);
        actualY = getResponsiveHeight(pos.y);
        actualWidth = getResponsiveWidth(width);
        actualHeight = getResponsiveHeight(height);

        // 提前100毫秒循环不了
        if (loop && player.getTime() >= (player.getDuration() - 500)) {
            player.seekTo(0);
        }

        if (player.isEnded() || player.isStopped()) {
            close();
            return;
        }
        tick++;

        if (isPlaying()) videoTexture = player.preRender();
        else return;

        // RENDER VIDEO
        if (player.isPlaying() || player.isPaused()) {
            renderTexture(guiGraphics, videoTexture);
        }

        // RENDER GIF
        if (!player.isPlaying()) {
            if (player.isPaused()) {
                renderIcon(guiGraphics, IMG_PAUSED);
            } else {
                renderIcon(guiGraphics, ImageAPI.loadingGif());
            }
        }

        renderStep10(guiGraphics, pPartialTick);
        renderStep30(guiGraphics, pPartialTick);

        // DEBUG RENDERING
        if (FMLLoader.isProduction()) {
            draw(guiGraphics, String.format("State: %s", player.getStateName()), getHeightCenter(-12));
            draw(guiGraphics, String.format("Time: %s (%s) / %s (%s)", FORMAT.format(new Date(player.getTime())), player.getTime(), FORMAT.format(new Date(player.getDuration())), player.getDuration()), getHeightCenter(0));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, float pPartialTick) {
        render(guiGraphics, 0, 0, pPartialTick);
    }

    private void renderTexture(GuiGraphics guiGraphics, int texture) {
        if (player.dimension() == null) return; // Checking if video available

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        drawTexture(guiGraphics, texture, actualX, actualY, actualWidth, actualHeight);
    }

    private int getHeightCenter(int offset) {
        return (int) ((actualHeight / 2) + offset);
    }

    private void renderIcon(GuiGraphics guiGraphics, ImageRenderer image) {
        int iconSize = 36;
        float xOffset = actualWidth - iconSize + actualX;
        float yOffset = actualHeight - iconSize + actualY;

        drawTexture(guiGraphics, image.texture(tick, 1, true), xOffset, yOffset, iconSize, iconSize);
    }

    private void renderStep30(GuiGraphics guiGraphics, float pPartialTicks) {
        if (fadeStep30 == 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int texture = IMG_STEP30.texture(tick, 1, true);
        float alpha = fadeStep30;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        float x = (actualWidth / 2 + 70 + actualX);
        float y = (actualHeight / 2 - 32 + actualY);
        int size = 64;

        drawTexture(guiGraphics, texture, x, y, size, size);

        fadeStep30 = Math.max(fadeStep30 - (pPartialTicks / 8), 0.0f);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        RenderSystem.disableBlend();
    }

    private void renderStep10(GuiGraphics guiGraphics, float pPartialTicks) {
        if (fadeStep10 == 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int texture = IMG_STEP10.texture(tick, 1, true);
        float alpha = fadeStep10;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        float x = (actualWidth / 2 - 134 + actualX);
        float y = (actualHeight / 2 - 32 + actualY);
        int size = 64;

        drawTexture(guiGraphics, texture, x, y, size, size);

        fadeStep10 = Math.max(fadeStep10 - (pPartialTicks / 8), 0.0f);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        RenderSystem.disableBlend();
    }

    private void drawTexture(GuiGraphics guiGraphics, int texture, float x, float y, float width, float height) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        bufferBuilder.addVertex(matrix4f, x, y + height, 0).setUv(0f, 1f);   // Bottom-left
        bufferBuilder.addVertex(matrix4f, x + width, y + height, 0).setUv(1f, 1f);  // Bottom-right
        bufferBuilder.addVertex(matrix4f, x + width, y, 0).setUv(1f, 0f);  // Top-right
        bufferBuilder.addVertex(matrix4f, x, y, 0).setUv(0f, 0f);   // Top-left

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        RenderSystem.disableBlend();
    }

    private void draw(GuiGraphics guiGraphics, String text, int height) {
        guiGraphics.drawString(minecraft.font, text, 5 + (int) actualX, height + (int) actualY, 0xffffff);
    }

    public void keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        // Up arrow key (Volume)
        if (pKeyCode == 265) {
            if (volume <= 95) volume += 5;
            else {
                volume = 100;
                float masterVolume = minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
                if (masterVolume <= 0.95)
                    minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(masterVolume + 0.05);
                else minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
            }

            float actualVolume = minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
            float newVolume = volume * actualVolume;
            ChatBox.LOGGER.info("Volume UP to: {}", newVolume);
            player.setVolume((int) newVolume);
        }

        // Down arrow key (Volume)
        if (pKeyCode == 264) {
            if (volume >= 5) {
                volume -= 5;
            } else volume = 0;
            float actualVolume = minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
            float newVolume = volume * actualVolume;
            ChatBox.LOGGER.info("Volume DOWN to: {}", newVolume);
            player.setVolume((int) newVolume);
        }

        // M to mute
        if (pKeyCode == 77) {
            if (!player.isMuted()) player.mute();
            else player.unmute();
        }

        // If control blocked can't modify the video time
        if (!canControl) return;

        // Right arrow key (Forwards)
        if (pKeyCode == 262) {
            // 如果设置循环且超出时长就跳到开头
            long time = loop && player.getTime() + 30000 > player.getDuration() ? 0 : player.getTime() + 30000;
            player.seekTo(time);
            fadeStep30 = 1;
        }

        // Left arrow key (Backwards)
        if (pKeyCode == 263) {
            player.seekTo(player.getTime() - 10000);
            fadeStep10 = 1;
        }

        // Space (Pause / Play)
        if (pKeyCode == 32) {
            if (!player.isPaused()) player.pause();
            else player.play();
        }
    }

    public void close() {
        if (started) {
            started = false;
            player.stop();
            //minecraft.getSoundManager().resume();
            GlStateManager._deleteTexture(videoTexture);
            player.release();
        }
    }
}
