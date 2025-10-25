package com.zhenshiz.chatbox;

import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.compat.terraentity.TerraEntityShop;
import com.zhenshiz.chatbox.data.ChatBoxTriggerCount;
import com.zhenshiz.chatbox.event.forge.SkipChatEvent;
import com.zhenshiz.chatbox.network.NetworkForge;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(ChatBox.MOD_ID)
public class ChatBoxForge {
    public ChatBoxForge() {
        ChatBox.init();
        if (ChatBox.PLATFORM.isModLoaded("terra_entity")) TerraEntityShop.register();
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onWorldLoad);
        MinecraftForge.EVENT_BUS.addListener(this::onChat);
        NetworkForge.registerHandlers();

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
            ChatBox.setTriggerCounts(world.getDataStorage().computeIfAbsent(nbt -> ChatBoxTriggerCount.fromNbt(world, nbt), () -> new ChatBoxTriggerCount(world), "chatbox_trigger_count"));
        }
    }

    private void onChat(SkipChatEvent event) {
        // 测试用
        System.out.println("SkipChatEvent: " + event.getResourceLocation() + " " + event.getGroup() + " " + event.getIndex() + " " + event.getTargets());
        if (!event.getTargets().isEmpty() && event.getPlayer() instanceof ServerPlayer p) {
            Entity entity = event.getTargets().get(0);
            ChatBoxCommandUtil.serverSetDialogBox(p, null, StrUtil.format("目标的uuid：{}，目标的主手物品：{}", entity.getUUID(), ((LivingEntity) entity).getMainHandItem()));
            if (event.getIndex() == 0) {
                ChatBoxCommandUtil.serverClearChatOption(p);
                ChatBoxCommandUtil.serverAddChatOption(p, "只是测试一下", "0", "提示", "COMMAND", "tell @p 你好");
            }
        }
    }
}
