package com.zhenshiz.chatbox.platform;

import com.zhenshiz.chatbox.event.forge.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.event.forge.SkipChatEvent;
import com.zhenshiz.chatbox.network.CustomPacket;
import com.zhenshiz.chatbox.network.NetworkForge;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.PacketDistributor;

import java.io.File;

public class ForgePlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {return "Forge";}

    @Override
    public boolean isModLoaded(String modId) {return ModList.get().isLoaded(modId);}

    @Override
    public boolean isDevelopmentEnvironment() {return !FMLLoader.isProduction();}

    @Override
    public File getGameDirectory() {return FMLLoader.getGamePath().toFile();}

    @Override
    public void runOnClient(Runnable runnable) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> runnable::run);
    }

    @Override
    public void sendToServer(CustomPacket packet) {
        NetworkForge.getChannel(packet).sendToServer(packet);
    }

    @Override
    public void sendToClient(ServerPlayer player, CustomPacket packet) {
        NetworkForge.getChannel(packet).send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @Override
    public boolean postRenderEventPre(GuiGraphics guiGraphics) {
        return MinecraftForge.EVENT_BUS.post(new ChatBoxRenderEvent.Pre(guiGraphics));
    }

    @Override
    public void postRenderEventPost(GuiGraphics guiGraphics) {
        MinecraftForge.EVENT_BUS.post(new ChatBoxRenderEvent.Post(guiGraphics));
    }

    @Override
    public void postSkipChatEvent(ChatBoxScreen chatBoxScreen, ResourceLocation resourceLocation, String group, Integer index) {
        MinecraftForge.EVENT_BUS.post(new SkipChatEvent(chatBoxScreen, resourceLocation, group, index));
    }
}
