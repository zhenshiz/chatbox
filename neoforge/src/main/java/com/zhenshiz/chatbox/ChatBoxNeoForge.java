package com.zhenshiz.chatbox;

import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.client.ClothLoader;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.neoforge.platform.NeoForgePlatformHelper;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@Mod(ChatBox.MOD_ID)
public class ChatBoxNeoForge {

    public ChatBoxNeoForge(ModContainer modContainer, Dist dist) {
        ChatBox.PLATFORM = new NeoForgePlatformHelper();
        ChatBox.init();
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onWorldLoad);

        if (dist.isClient()) {
            ChatBoxClient.init();
            if (ChatBox.isClothConfigLoaded()) modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> AutoConfigClient.getConfigScreen(ClothLoader.class, parent).get());
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ChatBoxCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    private void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        //只需要保存在主世界的data目录下即可
        if (levelAccessor instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) {
            ChatBox.setTriggerCounts(world.getDataStorage().computeIfAbsent(ChatBoxTriggerCount.getType()));
        }
    }
}
