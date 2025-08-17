package com.zhenshiz.chatbox;

import com.mojang.logging.LogUtils;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.platform.IPlatformHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.ServiceLoader;

public class ChatBox {
    public static final String MOD_ID = "chatbox";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Getter @Setter
    private static ChatBoxTriggerCount triggerCounts;

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz).findFirst().orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    public static ResourceLocation ResourceLocationMod(String path) {return new ResourceLocation(MOD_ID, path);}

    public static boolean isWaterMediaLoaded() {return PLATFORM.isModLoaded("watermedia");}
}
