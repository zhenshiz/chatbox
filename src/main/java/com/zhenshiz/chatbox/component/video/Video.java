package com.zhenshiz.chatbox.component.video;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.client.screen.ChatBoxScreen;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.data.ChatBoxDialogues;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public abstract class Video extends AbstractComponent<Video> {
    static final DateFormat FORMAT = new SimpleDateFormat("HH:mm:ss");
    static {
        FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-00:00"));
    }

    float actualX, actualY, actualWidth, actualHeight;
    // STATUS
    int tick = 0;
    float fadeStep30 = 0;
    float fadeStep10 = 0;
    boolean started;
    float volume = 100;
    // 是否成功播放，只要视频进入PLAYING状态，就为true，如果出现异常导致视频播放失败，就会在关闭视频时重新开始
    boolean success = false;
    int retry = 0;

    // CONTROL
    public boolean loop;
    public boolean canControl;
    public boolean canSkip;
    public boolean removeOnEnd = true;
    public boolean removeOnNext = true;

    // TOOLS
    static final Minecraft minecraft = Minecraft.getInstance();
    static ChatBoxScreen chatBoxScreen = ChatBoxUtil.chatBoxScreen;

    // VIDEO INFO
    final URI uri;
    int videoTexture = -1;

    static final ResourceLocation PAUSED = ChatBox.id("textures/video/paused.png");
    static final ResourceLocation STEP30 = ChatBox.id("textures/video/step30.png");
    static final ResourceLocation STEP10 = ChatBox.id("textures/video/step10.png");

    public Video(URI uri) {
        this.uri = uri;
    }

    public <T extends Video> T ofVideo(ChatBoxDialogues.Dialogues.Video video) {
        of(video).setId("video");
        //minecraft.getSoundManager().pause();
        this.canControl = video.canControl;
        this.canSkip = video.canSkip;
        this.loop = video.loop;
        this.removeOnEnd = video.removeOnEnd;
        this.removeOnNext = video.removeOnNext;
        ChatBox.LOGGER.info("Playing video ({}blocked) {} with volume: {}", canControl ? "not " : "", uri, realVolume());
        //noinspection unchecked
        return (T) this;
    }

    public boolean isPlaying() {return started;}

    public abstract boolean shouldRemove();

    /**视频当前时间，单位毫秒*/
    public abstract long time();

    /**视频总时长，单位毫秒*/
    public abstract long duration();

    /**用于控制视频循环和结束暂停*/
    public boolean isAboutToEnd() {return time() > 500 && time() >= duration() - 500;}

    public abstract boolean paused();

    public abstract void pause(boolean pause);

    public abstract void volume(int volume);

    public abstract boolean muted();

    public abstract void mute(boolean mute);

    public abstract void seek(long time);

    public int realVolume() {return (int) (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * volume);}

    void draw(GuiGraphics guiGraphics, String text, int centerOffset) {
        guiGraphics.drawString(minecraft.font, text, 5 + (int) actualX, centerOffset + (int) (actualHeight / 2 + actualY), 0xffffff);
    }

    void renderIconAtCenter(GuiGraphics graphics, ResourceLocation texture, int xOffset, int yOffset, int size) {
        if (!chatBoxScreen.isDebug()) return;
        float videoCX = actualX + actualWidth / 2;
        float videoCY = actualY + actualHeight / 2;
        float x = videoCX + (xOffset - size / 2f) * scale;
        float y = videoCY + (yOffset - size / 2f) * scale;
        drawTexture(graphics, texture, x, y, size, size);
    }

    void renderStep30(GuiGraphics guiGraphics, float pPartialTicks) {
        if (fadeStep30 == 0) return;
        renderIconAtCenter(guiGraphics, STEP30, 100, 0, 64);
        fadeStep30 = Math.max(fadeStep30 - (pPartialTicks / 8), 0.0f);
    }

    void renderStep10(GuiGraphics guiGraphics, float pPartialTicks) {
        if (fadeStep10 == 0) return;
        renderIconAtCenter(guiGraphics, STEP10, -100, 0, 64);
        fadeStep10 = Math.max(fadeStep10 - (pPartialTicks / 8), 0.0f);
    }

    void drawTexture(GuiGraphics guiGraphics, ResourceLocation texture, float x, float y, float width, float height) {
        RenderUtil.renderOpacity(guiGraphics, brightness, opacity, () ->
                RenderUtil.renderImage(guiGraphics, texture, x, y, width, height, scale, angle));
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

    abstract void stop();

    public abstract void close();

    static final Int2ObjectOpenHashMap<ResourceLocation> TEXTURES = new Int2ObjectOpenHashMap<>();

    static ResourceLocation getTextureId(int texture) {
        if (texture == -1) return null;
        return TEXTURES.computeIfAbsent(texture, i -> {
            var id = ChatBox.id("video_texture_" + i);
            minecraft.getTextureManager().register(id, new TextureWrapper(texture));
            return id;
        });
    }

    static class TextureWrapper extends AbstractTexture {
        public TextureWrapper(int id) {this.id = id;}

        @Override public int getId() {return id;}
        @Override public void load(ResourceManager resourceManager) {}
        @Override public void releaseId() {}
        @Override public void close() {}
    }
}
