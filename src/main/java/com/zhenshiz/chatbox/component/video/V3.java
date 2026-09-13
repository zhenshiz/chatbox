package com.zhenshiz.chatbox.component.video;

import net.minecraft.client.gui.GuiGraphics;
import org.watermedia.api.media.MRL;
import org.watermedia.api.media.MediaAPI;
import org.watermedia.api.media.players.MediaPlayer;

import java.net.URI;
import java.util.Date;

public class V3 extends Video {
    // TOOLS
    private final MRL mrl;
    private MediaPlayer player;

    public V3(URI uri) {
        super(uri);
        this.mrl = MediaAPI.mrl(uri);
    }

    @Override
    public boolean shouldRemove() {return (success || retry >= 5) && !started;}

    @Override
    public long time() {return player == null ? 0 : player.time();}

    @Override
    public long duration() {return player == null ? 0 : player.duration();}

    @Override
    public boolean paused() {return player == null || player.paused();}

    @Override
    public void pause(boolean pause) {if (player != null) player.pause(pause);}

    @Override
    public void volume(int volume) {if (player != null) player.volume(volume);}

    @Override
    public boolean muted() {return player == null || player.mute();}

    @Override
    public void mute(boolean mute) {if (player != null) player.mute(mute);}

    @Override
    public void seek(long time) {if (player != null) player.seek(time);}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
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
            drawTexture(guiGraphics, getTextureId(videoTexture), actualX, actualY, actualWidth, actualHeight);
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

    @Override
    void stop() {
        if (!success && retry < 5) { // 如果视频播放失败，就重新开始
            retry++;
            player.start();
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
            minecraft.getTextureManager().release(TEXTURES.remove(videoTexture));
            player.release();
        }
    }
}
