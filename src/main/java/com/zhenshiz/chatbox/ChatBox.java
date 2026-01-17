package com.zhenshiz.chatbox;

import com.mojang.logging.LogUtils;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.compat.plugin.PluginHelper;
import com.zhenshiz.chatbox.component.ComponentEvent;
//? >= 1.21
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.network.Packets;
import com.zhenshiz.chatbox.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
//? fabric {
import com.zhenshiz.chatbox.event.ChatBoxSettingLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
//?}
//? forge {
/*import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.compat.terraentity.TerraEntityShop;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
*///?}

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
//? !fabric
/*@Mod(ChatBox.MOD_ID)*/
public class ChatBox/*? fabric {*/ implements ModInitializer/*?}*/ {
    public static final String MOD_ID = "chatbox";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Platform PLATFORM = Platform.INSTANCE;
    @Getter
    private static ChatBoxTriggerCount triggerCounts;
    @Nullable
    public static PluginHelper pluginHelper;

    //? fabric
    public void onInitialize() {
    //? forge
    /*public ChatBox() {*/
        LOGGER.info("Ciallo～(∠·ω< )⌒★");
        ComponentEvent.registerDefaultEvents();
        Packets.register();
        //? fabric {
        ChatBoxSettingLoader.chatBoxLoader();
        CommandRegistrationCallback.EVENT.register(ChatBoxCommand::register);
        ServerWorldEvents.LOAD.register((server, world) -> {
            //只需要保存在主世界的data目录下即可
            if (world.dimension() == Level.OVERWORLD)
                //? >= 1.21 {
                triggerCounts = world.getDataStorage().computeIfAbsent(ChatBoxTriggerCount.factory(world), "chatbox_trigger_count");
                //?} else {
                /*triggerCounts = world.getDataStorage().computeIfAbsent(nbt -> ChatBoxTriggerCount.fromNbt(world, nbt), () -> new ChatBoxTriggerCount(world), "chatbox_trigger_count");*///?}
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((s, manager, bl) -> {
            //? >= 1.21
            ChatBoxDialoguesLoader.loadCriteria(s);
            s.getPlayerList().getPlayers().forEach(ChatBoxSettingLoader::initializeChatBoxScreen);
        });
        //? >= 1.21
        ServerLifecycleEvents.SERVER_STARTED.register(ChatBoxDialoguesLoader::loadCriteria);
    }
        //?}
    //? forge {
        /*if (ChatBox.PLATFORM.isModLoaded("terra_entity")) TerraEntityShop.register();
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onWorldLoad);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> {
            ChatBoxClient.init();
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> AutoConfig.getConfigScreen(Config.class, parent).get()));
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ChatBoxCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    private void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        //只需要保存在主世界的data目录下即可
        if (levelAccessor instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) {
            triggerCounts = world.getDataStorage().computeIfAbsent(nbt -> ChatBoxTriggerCount.fromNbt(world, nbt), () -> new ChatBoxTriggerCount(world), "chatbox_trigger_count");
        }
    }
    *///?}

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
