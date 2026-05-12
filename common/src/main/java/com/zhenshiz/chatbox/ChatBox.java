package com.zhenshiz.chatbox;

import com.mojang.logging.LogUtils;
import com.zhenshiz.chatbox.component.data.ComponentEvent;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.platform.IPlatformHelper;
import com.zhenshiz.chatbox.utils.mvel.MVELUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@SuppressWarnings({"SameParameterValue", "BooleanMethodIsAlwaysInverted"})
public class ChatBox {
    public static final String MOD_ID = "chatbox";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static IPlatformHelper PLATFORM;
    @Getter @Setter
    private static ChatBoxTriggerCount triggerCounts;
    @Nullable public static MinecraftServer server;

    public static void init() {
        LOGGER.info("Ciallo～(∠·ω< )⌒★");
        ComponentEvent.registerDefaultEvents();
        MVELUtil.init();
    }

    public static boolean isClothConfigLoaded() {
        return PLATFORM.isModLoaded("cloth_config") || PLATFORM.isModLoaded("cloth-config");
    }

    public static boolean isWaterMediaLoaded() {return PLATFORM.isModLoaded("watermedia");}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier parseId(String id) {return Identifier.parse(id);}
}
