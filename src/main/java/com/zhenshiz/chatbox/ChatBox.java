package com.zhenshiz.chatbox;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import com.zhenshiz.chatbox.command.ICommand;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

@Mod(ChatBox.MOD_ID)
public class ChatBox {
    public static final String MOD_ID = "chatbox";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChatBox(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        if (dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC, StrUtil.format("{}_config.toml", MOD_ID));
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    //注册指令
    private void onRegisterCommands(RegisterCommandsEvent event) {
        final String PACKAGE_NAME = "com.zhenshiz.chatbox.command";
        final String REGISTER = "register";
        ModContainer modContainer = ModList.get().getModContainerById(MOD_ID).orElseThrow();
        Set<ModFileScanData.ClassData> classes = modContainer.getModInfo().getOwningFile().getFile().getScanResult().getClasses();
        classes.forEach(classData -> {
            Type clazz = classData.clazz();
            if (clazz.getClassName().startsWith(PACKAGE_NAME)) {
                try {
                    ClassLoader classLoader = ChatBox.class.getClassLoader();
                    Class<?> commandClass = classLoader.loadClass(clazz.getClassName());
                    if (Arrays.stream(commandClass.getInterfaces()).toList().contains(ICommand.class)) {
                        Method register = commandClass.getMethod(REGISTER, CommandDispatcher.class, CommandBuildContext.class, Commands.CommandSelection.class);
                        Constructor<?> constructor = commandClass.getDeclaredConstructor();
                        Object iCommand = constructor.newInstance();
                        register.invoke(iCommand, event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    public static ResourceLocation ResourceLocationMod(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
