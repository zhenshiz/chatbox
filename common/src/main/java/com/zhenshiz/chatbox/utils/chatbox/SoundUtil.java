package com.zhenshiz.chatbox.utils.chatbox;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.mixin.client.SoundEngineAccessor;
import com.zhenshiz.chatbox.mixin.client.SoundInstanceAccessor;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**根据音效的名称（String）获取音效（SoundEvent以及SoundInstance）的工具*/
public class SoundUtil {
    public static final Minecraft minecraft = Minecraft.getInstance();

    public static SoundEvent fromString(String sound) {
        if (StrUtil.isEmpty(sound)) sound = "";
        return Holder.direct(SoundEvent.createVariableRangeEvent(ChatBox.parseId(sound))).value();
    }

    public static void playSound(String sound, float volume, float pitch) {
        if (StrUtil.isEmpty(sound)) return;
        if (minecraft.player != null) minecraft.player.playSound(fromString(sound), volume, pitch);
    }

    public static void playSound(String sound) {playSound(sound, 1.0F, 1.0F);}

    public static void stopSound(String sound) {
        if (StrUtil.isEmpty(sound)) return;
        getSoundManager().stop(ChatBox.parseId(sound), null);
    }

    public static SoundManager getSoundManager() {return minecraft.getSoundManager();}

    public static SoundEngine getSoundEngine() {
        return ((SoundEngineAccessor) minecraft.getSoundManager()).getSoundEngine();
    }

    public static Map<SoundInstance, ChannelAccess.ChannelHandle> getInstanceToChannel() {
        return ((SoundInstanceAccessor) getSoundEngine()).getInstanceToChannel();
    }

    public static void tickWhenPaused() {
        if (minecraft.isPaused()) ((SoundInstanceAccessor) getSoundEngine()).invokeTickNonPaused();
    }

    public static boolean isSoundActive(String sound) {
        for (SoundInstance soundInstance : getInstanceToChannel().keySet()) {
            if (soundInstance.getIdentifier().toString().equals(sound)) return true;
        }
        return false;
    }

    public static List<SoundInstance> getPlayingSounds(String sound) {
        List<SoundInstance> list = new ArrayList<>();
        for (SoundInstance soundInstance : getInstanceToChannel().keySet()) {
            if (soundInstance.getIdentifier().toString().equals(sound)) list.add(soundInstance);
        }
        return list;
    }
}
