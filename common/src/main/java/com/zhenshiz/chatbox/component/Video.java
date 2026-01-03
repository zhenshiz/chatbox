package com.zhenshiz.chatbox.component;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.FloatBlitRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.sounds.SoundSource;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.players.MediaPlayer;

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
    // 是否成功播放，只要视频进入PLAYING状态，就为true，如果出现异常导致视频播放失败，就会在关闭视频时重新开始
    private boolean success;

    // CONTROL
    private final boolean canControl;
    public final boolean canSkip;

    // TOOLS
    private final Minecraft minecraft = Minecraft.getInstance();
    private final MediaPlayer player;

    // VIDEO INFO
    WatermediaTexture videoTexture;

    public Video(URI uri, boolean canControl, boolean canSkip, boolean loop) {
        //minecraft.getSoundManager().pause();
        this.volume = 100;
        this.canControl = canControl;
        this.canSkip = canSkip;
        this.loop = loop;

        this.player = MRL.get(uri.toString()).createPlayer(Thread.currentThread(), minecraft, null, null, true, true);
        ChatBox.LOGGER.info("Playing video ({}blocked) ({} with volume: {}", canControl ? "not " : "", uri, (int) (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * 100));

        player.volume((int) (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * 100));
        started = true;
        player.start();
        success = false;
        videoTexture = new WatermediaTexture(player);
    }

    public boolean isPlaying() {return started;}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        renderInner(mouseX, mouseY);
        if (!isPlaying()) return;

        if (!success && player.playing()) success = true;
        tick++;
        if (player.ended() || player.stopped()) {
            if (loop) {
                player.start();
                return;
            }
            stop();
            return;
        }

        actualX = realX();
        actualY = realY();
        actualWidth = realWidth();
        actualHeight = realHeight();

        // RENDER VIDEO
        if (player.playing() || player.paused()) {
            drawTexture(guiGraphics, videoTexture, actualX, actualY, actualWidth, actualHeight, -1);
        }

        // RENDER GIF
/*        if (!player.playing()) {
            if (player.paused()) {
                renderIcon(guiGraphics, IMG_PAUSED);
            } else {
                renderIcon(guiGraphics, ImageAPI.loadingGif());
            }
        }

        renderStep10(guiGraphics, pPartialTick);
        renderStep30(guiGraphics, pPartialTick);*/

        // DEBUG RENDERING
        // if (!FMLLoader.isProduction()) {
        if (ChatBox.PLATFORM.isDevelopmentEnvironment()) {
            draw(guiGraphics, String.format("State: %s", player.status()), getHeightCenter(-12));
            draw(guiGraphics, String.format("Time: %s (%s) / %s (%s)", FORMAT.format(new Date(player.time())), player.time(), FORMAT.format(new Date(player.duration())), player.duration()), getHeightCenter(0));
        }
    }

    private int getHeightCenter(int offset) {
        return (int) ((actualHeight / 2) + offset);
    }

/*    private void renderIcon(GuiGraphics guiGraphics, ImageRenderer image) {
        int iconSize = 36;
        float xOffset = actualWidth - iconSize + actualX;
        float yOffset = actualHeight - iconSize + actualY;

        drawTexture(guiGraphics, image.texture(tick, 1, true), xOffset, yOffset, iconSize, iconSize, -1);
    }

    private void renderStep30(GuiGraphics guiGraphics, float pPartialTicks) {
        if (fadeStep30 == 0) return;
        int texture = IMG_STEP30.texture(tick, 1, true);
        float alpha = fadeStep30 * 100;
        float x = (actualWidth / 2 + 70 + actualX);
        float y = (actualHeight / 2 - 32 + actualY);
        int size = 64;

        drawTexture(guiGraphics, texture, x, y, size, size, RenderUtil.getColor(-1, alpha, 100));
        fadeStep30 = Math.max(fadeStep30 - (pPartialTicks / 8), 0.0f);
    }

    private void renderStep10(GuiGraphics guiGraphics, float pPartialTicks) {
        if (fadeStep10 == 0) return;
        int texture = IMG_STEP10.texture(tick, 1, true);
        float alpha = fadeStep10 * 100;
        float x = (actualWidth / 2 - 134 + actualX);
        float y = (actualHeight / 2 - 32 + actualY);
        int size = 64;

        drawTexture(guiGraphics, texture, x, y, size, size, RenderUtil.getColor(-1, alpha, 100));
        fadeStep10 = Math.max(fadeStep10 - (pPartialTicks / 8), 0.0f);
    }*/

    private void drawTexture(GuiGraphics guiGraphics, AbstractTexture texture, float x, float y, float width, float height, int color) {
        // todo 1.21.8还没修好
        guiGraphics.guiRenderState.submitGuiElement(new FloatBlitRenderState(guiGraphics, RenderPipelines.GUI_TEXTURED, TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()), guiGraphics.pose(), x, y, width, height, 1, 1, color));
    }

    private void draw(GuiGraphics guiGraphics, String text, int height) {
        guiGraphics.drawString(minecraft.font, text, 5 + (int) actualX, height + (int) actualY, -1);
    }

    public void keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        // Up arrow key (Volume)
        if (pKeyCode == 265) {
            if (volume <= 95) volume += 5;
            else {
                volume = 100;
                float masterVolume = minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
                if (masterVolume <= 0.95) minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(masterVolume + 0.05);
                else minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
            }

            float actualVolume = minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
            float newVolume = volume * actualVolume;
            ChatBox.LOGGER.info("Volume UP to: {}", newVolume);
            player.volume((int) newVolume);
        }

        // Down arrow key (Volume)
        if (pKeyCode == 264) {
            if (volume >= 5) {
                volume -= 5;
            } else volume = 0;
            float actualVolume = minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
            float newVolume = volume * actualVolume;
            ChatBox.LOGGER.info("Volume DOWN to: {}", newVolume);
            player.volume((int) newVolume);
        }

        // M to mute
        if (pKeyCode == 77) {
            player.mute(!player.mute());
        }

        // If control blocked can't modify the video time
        if (!canControl) return;

        // Right arrow key (Forwards)
        if (pKeyCode == 262) {
            // 如果设置循环且超出时长就跳到开头
            long time = loop && player.time() + 30000 > player.duration() ? 0 : player.time() + 30000;
            player.seek(time);
            fadeStep30 = 1;
        }

        // Left arrow key (Backwards)
        if (pKeyCode == 263) {
            player.seek(player.time() - 10000);
            fadeStep10 = 1;
        }

        // Space (Pause / Play)
        if (pKeyCode == 32) {
            player.pause(!player.pause());
        }
    }

    private void stop() {
        if (!success) { // 如果视频播放失败，就重新开始
            player.start();
            return;
        }
        close();
    }

    public void close() {
        if (started) {
            // 视频正常播放结束，触发ON_END事件
            fireEvent("ON_END");
            started = false;
            player.stop();
            //minecraft.getSoundManager().resume();
            player.release();
        }
    }

    public static class WatermediaTexture extends AbstractTexture {
        private final MediaPlayer player;
        private NativeImage cachedImage; // 用于中转的像素缓存
        private int lastKnownTextureId = -1; // 用于检测Watermedia纹理是否已更新

        public WatermediaTexture(MediaPlayer player) {
            this.player = player;
            // 初始化时创建一个空的NativeImage占位，尺寸后续调整
            this.cachedImage = new NativeImage(1, 1, false);
            this.createTexture();
        }

        private void createTexture() {
            GpuDevice device = RenderSystem.getDevice();
            this.texture = device.createTexture(() -> "watermedia_texture", 5,
                    TextureFormat.RGBA8, cachedImage.getWidth(), cachedImage.getHeight(), 1, 1
            );
            this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
            this.textureView = device.createTextureView(this.texture);
        }

        // **核心方法：从Watermedia的OpenGL纹理同步数据**
        public void updateFromPlayer() {
            int currentGlTexture = player.texture();
            if (currentGlTexture != this.lastKnownTextureId) {
                this.lastKnownTextureId = currentGlTexture;

                // 1. 从OpenGL纹理读取数据到NativeImage
                updateNativeImageFromGL(currentGlTexture);

                // 2. 上传到GpuTexture
                if (this.texture != null) {
                    RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.cachedImage);
                }
            }
        }

        private void updateNativeImageFromGL(int glTextureId) {
            // **这是技术关键点，需要你实现**
            int width = player.width();
            int height = player.height();

            // 如果尺寸变化，重新创建NativeImage
            if (cachedImage == null || cachedImage.getWidth() != width || cachedImage.getHeight() != height) {
                if (cachedImage != null) cachedImage.close();
                this.cachedImage = new NativeImage(width, height, false);
            }

            // 方案1：使用glGetTexImage（标准OpenGL，但可能较慢）
            // RenderSystem.bindTexture(glTextureId);
            // glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, cachedImage.getPointer());

            // 方案2：使用更高效的PBO（Pixel Buffer Object）进行异步读取
            // 推荐方案2以提升性能，但实现更复杂
        }

        @Override
        public void close() {
            if (cachedImage != null) {
                cachedImage.close();
                cachedImage = null;
            }
            super.close(); // 释放GpuTexture和GpuTextureView
        }
    }
}
