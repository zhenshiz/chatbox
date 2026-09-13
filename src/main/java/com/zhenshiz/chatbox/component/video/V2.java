package com.zhenshiz.chatbox.component.video;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.GuiGraphics;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.player.videolan.VideoPlayer;
import org.watermedia.videolan4j.player.base.State;

import java.net.URI;
import java.util.Date;

public class V2 extends Video {
    // TOOLS
    private final VideoPlayer player;

    public V2(URI uri) {
        super(uri);
        this.player = new VideoPlayer(minecraft);
        volume(realVolume());
        started = true;
        player.start(uri);
    }

    @Override
    public boolean shouldRemove() {return !started;}

    @Override
    public long time() {return player.getTime();}

    @Override
    public long duration() {return player.getDuration();}

    @Override
    public boolean paused() {return player.isPaused();}

    @Override
    public void pause(boolean pause) {
        if (pause) { if (!paused()) player.pause(); } else if (paused()) player.resume();
    }

    @Override
    public void volume(int volume) {player.setVolume(volume);}

    @Override
    public boolean muted() {return player.isMuted();}

    @Override
    public void mute(boolean mute) {if (mute) player.mute(); else player.unmute();}

    @Override
    public void seek(long time) {player.seekTo(time);}

    public State getState() {
        var raw = player.raw();
        if (raw == null) return null;
        return raw.mediaPlayer().status().state();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        renderInner(mouseX, mouseY);
        if (!isPlaying()) return;

        if (!success && getState() == State.PLAYING) { success = true; retry = 0; }
        tick++;
        if (isAboutToEnd()) {
            if (loop) seek(0);
            else if (!removeOnEnd) pause(true);
        }
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
        if (player.isPlaying() || paused()) {
            if (player.dimension() == null) return; // Checking if video available
            drawTexture(guiGraphics, getTextureId(videoTexture), actualX, actualY, actualWidth, actualHeight);
        }

        // RENDER GIF
        if (!player.isPlaying()) {
            if (paused())
                renderIconAtCenter(guiGraphics, PAUSED, (int) (actualWidth / 2 - 18), (int) (actualHeight / 2 - 18), 36);
            else {
                var texture = getTextureId(ImageAPI.loadingGif().texture(tick, pPartialTick, true));
                renderIconAtCenter(guiGraphics, texture, (int) (actualWidth / 2 - 18), (int) (actualHeight / 2 - 18), 36);
            }
        }

        renderStep10(guiGraphics, pPartialTick);
        renderStep30(guiGraphics, pPartialTick);

        // DEBUG RENDERING
        if (chatBoxScreen.isDebug()) {
            draw(guiGraphics, String.format("State: %s", player.getStateName()), -12);
            draw(guiGraphics, String.format("Time: %s (%s) / %s (%s)", FORMAT.format(new Date(time())), time(), FORMAT.format(new Date(duration())), duration()), 0);
        }
    }

    @Override
    void stop() {
        if (!success && retry < 5) { // 如果视频播放失败，就重新开始
            retry++;
            player.start(uri);
            return;
        }
        close();
    }

    @Override
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
}
