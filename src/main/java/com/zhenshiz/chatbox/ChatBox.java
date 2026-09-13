package com.zhenshiz.chatbox;

import com.mojang.logging.LogUtils;
import com.zhenshiz.chatbox.compat.plugin.PluginHelper;
import com.zhenshiz.chatbox.component.data.ComponentEvent;
import com.zhenshiz.chatbox.data.ChatBoxSavedData;
import com.zhenshiz.chatbox.network.Packets;
import com.zhenshiz.chatbox.platform.Platform;
import com.zhenshiz.chatbox.utils.mvel.MVELUtil;
import net.minecraft.resources.ResourceLocation;
//? >= 1.21
import net.minecraft.server.MinecraftServer;
import lombok.Setter;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
//? fabric {
import com.zhenshiz.chatbox.event.ChatBoxServerEvents;
import net.fabricmc.api.ModInitializer;
//?}
//? forge {
/*import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.compat.terraentity.TerraEntityShop;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.common.Mod;
*///?}

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
//? !fabric
/*@Mod(ChatBox.MOD_ID)*/
public class ChatBox/*? fabric {*/ implements ModInitializer/*?}*/ {
    public static final String MOD_ID = "chatbox";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Platform PLATFORM = Platform.INSTANCE;
    @Getter @Setter
    private static ChatBoxSavedData savedData;
    @Nullable
    public static PluginHelper pluginHelper;
    //? >= 1.21
    @Nullable public static MinecraftServer server;
    public static Boolean useWaterMediaV3;

    //? fabric
    public void onInitialize() {
    //? forge
    /*public ChatBox() {*/
        LOGGER.info("Ciallo～(∠·ω< )⌒★");
        ComponentEvent.registerDefaultEvents();
        Packets.register();
        MVELUtil.init();
        //? fabric
        ChatBoxServerEvents.init();
        //? forge {
        /*if (PLATFORM.isModLoaded("terra_entity")) TerraEntityShop.register();
        if (FMLEnvironment.dist.isClient()) {
            ChatBoxClient.init();
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> AutoConfig.getConfigScreen(Config.class, parent).get()));
        }*///?}
        String waterMediaVersion = PLATFORM.getModVersion("watermedia");
        if (waterMediaVersion.isEmpty()) useWaterMediaV3 = null;
        else useWaterMediaV3 = waterMediaVersion.startsWith("3.");
    }

    public static ResourceLocation parseId(String id) {
        //? >= 1.21 {
        return ResourceLocation.parse(id);
        //?} else {
        /*return new ResourceLocation(id);
         *///?}
    }

    public static ResourceLocation id(String path) {
        return parseId(MOD_ID + ":" + path);
    }

    public static boolean isModLoaded(String modId) {
        return PLATFORM.isModLoaded(modId);
    }

    public static boolean isWaterMediaLoaded() {return isModLoaded("watermedia");}
}
