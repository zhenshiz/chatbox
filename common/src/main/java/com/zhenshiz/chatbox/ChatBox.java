package com.zhenshiz.chatbox;

import com.mojang.logging.LogUtils;
import com.zhenshiz.chatbox.component.ComponentEvent;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.platform.IPlatformHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.ServiceLoader;

@SuppressWarnings({"SameParameterValue", "BooleanMethodIsAlwaysInverted"})
public class ChatBox {
    public static final String MOD_ID = "chatbox";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Getter @Setter
    private static ChatBoxTriggerCount triggerCounts;

    public static void init() {
        LOGGER.info("Ciallo～(∠·ω< )⌒★");
        ComponentEvent.registerDefaultEvents();
    }

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz).findFirst().orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    //todo 修改所有元素透明度的渲染方式
    public static boolean isWaterMediaLoaded() {return false /*PLATFORM.isModLoaded("watermedia")*/;}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
