package com.zhenshiz.chatbox.component;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.client.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.MediaAPI;
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
    private float volume = 100;
    // 是否成功播放，只要视频进入PLAYING状态，就为true，如果出现异常导致视频播放失败，就会在关闭视频时重新开始
    private boolean success = false;
    private int retry = 0;

    // CONTROL
    public boolean loop;
    public boolean canControl;
    public boolean canSkip;
    public boolean removeOnEnd = true;
    public boolean removeOnNext = true;

    // TOOLS
    static final Minecraft minecraft = Minecraft.getInstance();
    static ChatBoxScreen chatBoxScreen = ChatBoxUtil.chatBoxScreen;
    private final MRL mrl;
    private MediaPlayer player;

    // VIDEO INFO
    final URI uri;
    int videoTexture = -1;

    static final Identifier PAUSED = ChatBox.id("textures/video/paused.png");
    static final Identifier STEP30 = ChatBox.id("textures/video/step30.png");
    static final Identifier STEP10 = ChatBox.id("textures/video/step10.png");

    public Video(URI uri) {
        this.uri = uri;
        this.mrl = MediaAPI.mrl(uri);
    }

    public Video ofVideo(ChatBoxDialogues.Dialogues.Video video) {
        of(video).setId("video");
        //minecraft.getSoundManager().pause();
        this.canControl = video.canControl;
        this.canSkip = video.canSkip;
        this.loop = video.loop;
        this.removeOnEnd = video.removeOnEnd;
        this.removeOnNext = video.removeOnNext;
        ChatBox.LOGGER.info("Playing video ({}blocked) {} with volume: {}", canControl ? "not " : "", uri, realVolume());
        return this;
    }

    public boolean isPlaying() {return started;}

    public boolean shouldClose() {return (success || retry >= 5) && !started;}

    public long time() {return player == null ? 0 : player.time();}

    public long duration() {return player == null ? 0 : player.duration();}

    public boolean isAboutToEnd() {return time() > 500 && time() >= duration() - 500;}

    public boolean paused() {return player == null || player.paused();}

    public void pause(boolean pause) {if (player != null) player.pause(pause);}

    public void volume(int volume) {if (player != null) player.volume(volume);}

    public boolean muted() {return player == null || player.mute();}

    public void mute(boolean mute) {if (player != null) player.mute(mute);}

    public void seek(long time) {if (player != null) player.seek(time);}

    public int realVolume() {return (int) (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * volume);}

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        renderInner(mouseX, mouseY);
        if (!started) {
            if (mrl.status().loaded()) {
                if (player == null) {
                    player = MediaAPI.createPlayer(mrl, () -> MediaAPI.glEngine(Thread.currentThread(), minecraft), MediaAPI::alEngine);
                } else {
                    volume(realVolume());
                    started = true;
                    player.start();
                }
            }
            return;
        }

        if (!success && player.playing()) { success = true; retry = 0; }
        tick++;
        if (isAboutToEnd()) {
            if (loop) seek(0);
            else if (!removeOnEnd) pause(true);
        }
        if (player.error() || player.ended() || player.stopped()) {
            stop();
            return;
        }

        actualX = realX();
        actualY = realY();
        actualWidth = realWidth();
        actualHeight = realHeight();

        videoTexture = (int) player.texture();

        // RENDER VIDEO
        if (player.playing() || paused()) {
            drawTexture(guiGraphics, getTextureId(videoTexture, player.width(), player.height()), actualX, actualY, actualWidth, actualHeight);
        }

        // RENDER GIF
        if (!player.playing()) {
            if (paused())
                renderIconAtCenter(guiGraphics, PAUSED, (int) (actualWidth / 2 - 18), (int) (actualHeight / 2 - 18), 36);
/*            else {
                var texture = getTextureId(ImageAPI.loadingGif().texture(tick, pPartialTick, true), 36, 36);
                renderIconAtCenter(guiGraphics, texture, (int) (actualWidth / 2 - 18), (int) (actualHeight / 2 - 18), 36);
            }*/
        }

        renderStep10(guiGraphics, pPartialTick);
        renderStep30(guiGraphics, pPartialTick);

        // DEBUG RENDERING
        if (chatBoxScreen.isDebug()) {
            draw(guiGraphics, String.format("State: %s", player.status()), -12);
            draw(guiGraphics, String.format("Time: %s (%s) / %s (%s)", FORMAT.format(new Date(time())), time(), FORMAT.format(new Date(duration())), duration()), 0);
        }
    }

    private void draw(GuiGraphicsExtractor guiGraphics, String text, int centerOffset) {
        guiGraphics.text(minecraft.font, text, 5 + (int) actualX, centerOffset + (int) (actualHeight / 2 + actualY), -1);
    }

    private void renderIconAtCenter(GuiGraphicsExtractor graphics, Identifier texture, int xOffset, int yOffset, int size) {
        if (!chatBoxScreen.isDebug()) return;
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

    private void drawTexture(GuiGraphicsExtractor guiGraphics, Identifier texture, float x, float y, float width, float height) {
        RenderUtil.renderImage(guiGraphics, texture, x, y, width, height, scale, opacity, brightness, angle);
    }

    public void keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        // Up arrow key (Volume)
        if (pKeyCode == 265) {
            if (volume <= 95) volume += 5; else volume = 100;
            int newVolume = realVolume();
            ChatBox.LOGGER.info("Volume UP to: {}", newVolume);
            volume(newVolume);
        }

        // Down arrow key (Volume)
        if (pKeyCode == 264) {
            if (volume >= 5) volume -= 5; else volume = 0;
            int newVolume = realVolume();
            ChatBox.LOGGER.info("Volume DOWN to: {}", newVolume);
            volume(newVolume);
        }

        // M to mute
        if (pKeyCode == 77) mute(!muted());

        // If control blocked can't modify the video time
        if (!canControl) return;

        // Right arrow key (Forwards)
        if (pKeyCode == 262) {
            // 如果设置循环且超出时长就跳到开头
            long time = loop && time() + 30000 > duration() ? 0 : time() + 30000;
            seek(time);
            fadeStep30 = 1;
        }

        // Left arrow key (Backwards)
        if (pKeyCode == 263) {
            seek(time() - 10000);
            fadeStep10 = 1;
        }

        // Space (Pause / Play)
        if (pKeyCode == 32) pause(!paused());
    }

    private void stop() {
        if (!success && retry < 5) { // 如果视频播放失败，就重新开始
            retry++;
            player.start();
            return;
        }
        close(false);
    }

    public void close(boolean force) {
        if (started) {
            // 视频正常播放结束，触发ON_END事件
            if (!force) fireEvent("ON_END");
            started = false;
            player.stop();
            //minecraft.getSoundManager().resume();
            minecraft.getTextureManager().release(TEXTURES.remove(videoTexture));
            player.release();
        }
    }

    private static final Int2ObjectOpenHashMap<Identifier> TEXTURES = new Int2ObjectOpenHashMap<>();

    private static Identifier getTextureId(int texture, int width, int height) {
        if (texture <= 0) return null;
        var manager = minecraft.getTextureManager();
        var textureId = TEXTURES.computeIfAbsent(texture, i -> {
            var id = ChatBox.id("video_texture_" + i);
            manager.register(id, new VideoFrameTexture());
            return id;
        });
        return manager.getTexture(textureId) instanceof VideoFrameTexture frame && frame.update(texture, width, height) ?
                textureId : null;
    }

    static final class VideoGlTexture extends GlTexture {
        private boolean disposed;

        VideoGlTexture(int glId, int width, int height) {
            super(GpuTexture.USAGE_TEXTURE_BINDING, "chatbox_video_texture", TextureFormat.RGBA8, width, height, 1, 1, glId);
        }

        @Override
        public void close() {disposed = true;}

        @Override
        public boolean isClosed() {return disposed;}
    }

    static final class VideoFrameTexture extends AbstractTexture {
        private int wrappedGlId = -1;
        private int wrappedWidth = -1;
        private int wrappedHeight = -1;

        boolean update(int glId, int width, int height) {
            if (texture != null && glId == wrappedGlId && width == wrappedWidth && height == wrappedHeight) {
                return true;
            }
            disposeGpu();
            try {
                var wrapped = new VideoGlTexture(glId, width, height);
                this.texture = wrapped;
                this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                this.textureView = RenderSystem.getDevice().createTextureView(wrapped);
                wrappedGlId = glId;
                wrappedWidth = width;
                wrappedHeight = height;
                return true;
            } catch (Throwable t) {
                ChatBox.LOGGER.error("Failed to wrap video GL texture {}: {}", glId, t.toString());
                disposeGpu();
                return false;
            }
        }

        private void disposeGpu() {
            if (textureView != null) {
                try {
                    textureView.close();
                } catch (Throwable ignored) {
                }
                textureView = null;
            }
            if (texture != null) {
                try {
                    texture.close();
                } catch (Throwable ignored) {
                }
                texture = null;
            }
            sampler = null;
            wrappedGlId = -1;
            wrappedWidth = -1;
            wrappedHeight = -1;
        }

        @Override
        public void close() {disposeGpu();}
    }
}
