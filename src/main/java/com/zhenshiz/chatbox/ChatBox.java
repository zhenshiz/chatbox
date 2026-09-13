package com.zhenshiz.chatbox;

import com.mojang.logging.LogUtils;
import com.zhenshiz.chatbox.compat.plugin.PluginHelper;
import com.zhenshiz.chatbox.compat.terraentity.TerraEntityShop;
import com.zhenshiz.chatbox.component.data.ComponentEvent;
import com.zhenshiz.chatbox.data.ChatBoxSavedData;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import com.zhenshiz.chatbox.utils.mvel.MVELUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@SuppressWarnings({"SameParameterValue", "BooleanMethodIsAlwaysInverted"})
@Mod(ChatBox.MOD_ID)
public class ChatBox {
    public static final String MOD_ID = "chatbox";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Getter @Setter
    private static ChatBoxSavedData savedData;
    @Nullable
    public static PluginHelper pluginHelper;
    public static Boolean useWaterMediaV3;

    public ChatBox(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        ComponentEvent.registerDefaultEvents();
        MVELUtil.init();
        if (isTerraEntityLoaded()) TerraEntityShop.register();

        if (dist.isClient()) {
            modContainer.registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC, StrUtil.format("{}_config.toml", MOD_ID));
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

            String waterMediaVersion = getModVersion("watermedia");
            if (waterMediaVersion.isEmpty()) useWaterMediaV3 = null;
            else useWaterMediaV3 = waterMediaVersion.startsWith("3.");
        }
    }

    public static ResourceLocation parseId(String id) {
        return ResourceLocation.parse(id);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static String getModVersion(String modId) {
        var modInfo = ModList.get().getModFileById(modId);
        return modInfo == null ? "" : modInfo.versionString();
    }

    public static boolean isWaterMediaLoaded() {
        return isModLoaded("watermedia");
    }

    public static boolean isTerraEntityLoaded() {
        return isModLoaded("terra_entity");
    }

    public static boolean isTextAnimatorLoaded() {
        return isModLoaded("textanimator");
    }
}
