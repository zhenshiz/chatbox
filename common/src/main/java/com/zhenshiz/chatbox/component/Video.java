package com.zhenshiz.chatbox.component;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.player.videolan.VideoPlayer;
import org.watermedia.videolan4j.player.base.State;

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
    private boolean success = false;
    private int retry = 0;

    // CONTROL
    private final boolean canControl;
    public final boolean canSkip;

    // TOOLS
    private static final Minecraft minecraft = Minecraft.getInstance();
    private final VideoPlayer player;

    // VIDEO INFO
    private final URI uri;
    int videoTexture = -1;

    static final Identifier PAUSED = ChatBox.id("textures/video/paused.png");
    static final Identifier STEP30 = ChatBox.id("textures/video/step30.png");
    static final Identifier STEP10 = ChatBox.id("textures/video/step10.png");

    public Video(URI uri, boolean canControl, boolean canSkip, boolean loop) {
        //minecraft.getSoundManager().pause();
        this.volume = 100;
        this.canControl = canControl;
        this.canSkip = canSkip;
        this.loop = loop;
        this.uri = uri;

        this.player = new VideoPlayer(minecraft);
        ChatBox.LOGGER.info("Playing video ({}blocked) ({} with volume: {}", canControl ? "not " : "", uri, (int) (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * 100));

        player.setVolume((int) (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * 100));
        started = true;
        player.start(uri);
        setId("video");
    }

    public boolean isPlaying() {return started;}

    public State getState() {
        var raw = player.raw();
        if (raw == null) return null;
        return raw.mediaPlayer().status().state();
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        renderInner(mouseX, mouseY);
        if (!isPlaying()) return;

        if (!success && getState() == State.PLAYING) { success = true; retry = 0; }
        tick++;
        if (loop && player.getTime() >= player.getDuration() - 500) player.seekTo(0);
        if (player.isBroken() || player.isEnded() || player.isStopped()) {
            stop();
            return;
        }

        actualX = realX();
        actualY = realY();
        actualWidth = realWidth();
        actualHeight = realHeight();

        videoTexture = player.preRender();

        // RENDER VIDEO
        if (player.isPlaying() || player.isPaused()) {
            if (player.dimension() == null) return; // Checking if video available
            drawTexture(guiGraphics, videoTexture, actualX, actualY, actualWidth, actualHeight);
        }

        // RENDER GIF
        if (!player.isPlaying()) {
            if (player.isPaused())
                renderIconAtCenter(guiGraphics, PAUSED, (int) (actualWidth / 2 - 18), (int) (actualHeight / 2 - 18), 36);
            else {
                var texture = getTextureId(ImageAPI.loadingGif().texture(tick, pPartialTick, true), 36, 36);
                renderIconAtCenter(guiGraphics, texture, (int) (actualWidth / 2 - 18), (int) (actualHeight / 2 - 18), 36);
            }
        }

        renderStep10(guiGraphics, pPartialTick);
        renderStep30(guiGraphics, pPartialTick);

        // DEBUG RENDERING
        if (ChatBox.PLATFORM.isDevelopmentEnvironment()) {
            draw(guiGraphics, String.format("State: %s", player.getStateName()), -12);
            draw(guiGraphics, String.format("Time: %s (%s) / %s (%s)", FORMAT.format(new Date(player.getTime())), player.getTime(), FORMAT.format(new Date(player.getDuration())), player.getDuration()), 0);
        }
    }

    private void draw(GuiGraphicsExtractor guiGraphics, String text, int centerOffset) {
        guiGraphics.text(minecraft.font, text, 5 + (int) actualX, centerOffset + (int) (actualHeight / 2 + actualY), 0xffffff);
    }

    private void renderIconAtCenter(GuiGraphicsExtractor graphics, Identifier texture, int xOffset, int yOffset, int size) {
        float videoCX = actualX + actualWidth / 2;
        float videoCY = actualY + actualHeight / 2;
        float x = videoCX + (xOffset - size / 2f) * scale;
        float y = videoCY + (yOffset - size / 2f) * scale;
        drawTexture(graphics, texture, x, y, size, size);
    }

    private void renderStep30(GuiGraphicsExtractor guiGraphics, float pPartialTicks) {
        if (fadeStep30 == 0) return;
        renderIconAtCenter(guiGraphics, STEP30, 100, 0, 64);
        fadeStep30 = Math.max(fadeStep30 - (pPartialTicks / 8), 0.0f);
    }

    private void renderStep10(GuiGraphicsExtractor guiGraphics, float pPartialTicks) {
        if (fadeStep10 == 0) return;
        renderIconAtCenter(guiGraphics, STEP10, -100, 0, 64);
        fadeStep10 = Math.max(fadeStep10 - (pPartialTicks / 8), 0.0f);
    }

    private void drawTexture(GuiGraphicsExtractor guiGraphics, int texture, float x, float y, float width, float height) {
        drawTexture(guiGraphics, getTextureId(texture, (int) width, (int) height), x, y, width, height);
    }
    private void drawTexture(GuiGraphicsExtractor guiGraphics, Identifier texture, float x, float y, float width, float height) {
        RenderUtil.renderImage(guiGraphics, texture, x, y, width, height, scale, opacity, brightness, angle);
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

    private void stop() {
        if (!success && retry < 5) { // 如果视频播放失败，就重新开始
            retry++;
            player.start(uri);
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
            GlStateManager._deleteTexture(videoTexture);
            player.release();
        }
    }

    private static final Int2ObjectOpenHashMap<Identifier> TEXTURES = new Int2ObjectOpenHashMap<>();

    private static Identifier getTextureId(int texture, int width, int height) {
        if (texture == -1) return null;
        return TEXTURES.computeIfAbsent(texture, i -> {
            var id = ChatBox.id("video_texture_" + i);
            minecraft.getTextureManager().register(id, new TextureWrapper(texture, width, height));
            return id;
        });
    }

    private static class TextureWrapper extends AbstractTexture {
        public TextureWrapper(int id, int width, int height) {
            this.texture = new GlTexture(GpuTexture.USAGE_TEXTURE_BINDING, "texturewrapper_" + id, TextureFormat.RGBA8, width, height, 1, 1, id) {};
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
        }
    }
}
