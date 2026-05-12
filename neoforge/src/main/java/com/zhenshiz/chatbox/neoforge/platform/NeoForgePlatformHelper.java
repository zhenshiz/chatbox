package com.zhenshiz.chatbox.neoforge.platform;

import com.zhenshiz.chatbox.neoforge.event.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.neoforge.event.SkipChatEvent;
import com.zhenshiz.chatbox.platform.IPlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.File;
import java.util.List;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public boolean isModLoaded(String modId) {return ModList.get().isLoaded(modId);}

    @Override
    public boolean isDevelopmentEnvironment() {return !FMLLoader.getCurrent().isProduction();}

    @Override
    public boolean isClient() {return FMLEnvironment.getDist().isClient();}

    @Override
    public File getGameDirectory() {return FMLLoader.getCurrent().getGameDir().toFile();}

    @Override
    public void sendToServer(CustomPacketPayload packet) {
        if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.connection.send(packet);
    }

    @Override
    public void sendToClient(ServerPlayer player, CustomPacketPayload packet) {PacketDistributor.sendToPlayer(player, packet);}

    @Override
    public boolean postRenderEventPre(GuiGraphicsExtractor guiGraphics) {
        return NeoForge.EVENT_BUS.post(new ChatBoxRenderEvent.Pre(guiGraphics)).isCanceled();
    }

    @Override
    public void postRenderEventPost(GuiGraphicsExtractor guiGraphics) {
        NeoForge.EVENT_BUS.post(new ChatBoxRenderEvent.Post(guiGraphics));
    }

    @Override
    public void postSkipChatEvent(Player player, Identifier identifier, String group, Integer index, List<Entity> targets) {
        NeoForge.EVENT_BUS.post(new SkipChatEvent(player, identifier, group, index, targets));
    }
}
