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
    public static IPlatformHelper PLATFORM;

    public static void init() {
        LOGGER.info("Ciallo～(∠·ω< )⌒★");
        PLATFORM = load();
        ComponentEvent.registerDefaultEvents();
    }

    private static IPlatformHelper load() {
        boolean isFabric;
        try {
            Class.forName("net.neoforged.neoforge.common.NeoForge");
            isFabric = false;
        } catch (ClassNotFoundException e) {
            isFabric = true;
        }
        var loaded = ServiceLoader.load(IPlatformHelper.class);
        for (var service : loaded) {
            if (isFabric) {
                if (service.getPlatformName().equals("Fabric")) return service;
            } else if (service.getPlatformName().equals("NeoForge")) return service;
        }
        throw new NullPointerException("Failed to load service for " + IPlatformHelper.class.getName());
    }

    public static boolean isClothConfigLoaded() {
        return PLATFORM.isModLoaded("cloth_config") || PLATFORM.isModLoaded("cloth-config");
    }

    //todo 修改所有元素透明度的渲染方式
    public static boolean isWaterMediaLoaded() {return false /*PLATFORM.isModLoaded("watermedia")*/;}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
