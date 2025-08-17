package com.zhenshiz.chatbox.platform;

import com.zhenshiz.chatbox.event.fabric.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.event.fabric.SkipChatEvent;
import com.zhenshiz.chatbox.network.CustomPacket;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {return "Fabric";}

    @Override
    public boolean isModLoaded(String modId) {return FabricLoader.getInstance().isModLoaded(modId);}

    @Override
    public boolean isDevelopmentEnvironment() {return FabricLoader.getInstance().isDevelopmentEnvironment();}

    @Override
    public File getGameDirectory() {return FabricLoader.getInstance().getGameDir().toFile();}

    @Override
    public void runOnClient(Runnable runnable) {
        Minecraft instance = Minecraft.getInstance();
        instance.execute(runnable);
    }

    @Override
    public void sendToServer(CustomPacket packet) {
        var buf = PacketByteBufs.create();
        packet.write(buf);
        ClientPlayNetworking.send(packet.id(), buf);
    }

    @Override
    public void sendToClient(ServerPlayer player, CustomPacket packet) {
        var buf = PacketByteBufs.create();
        packet.write(buf);
        ServerPlayNetworking.send(player, packet.id(), buf);
    }

    @Override
    public boolean postRenderEventPre(GuiGraphics guiGraphics) {
        return ChatBoxRenderEvent.PRE.invoker().pre(guiGraphics);
    }

    @Override
    public void postRenderEventPost(GuiGraphics guiGraphics) {
        ChatBoxRenderEvent.POST.invoker().post(guiGraphics);
    }

    @Override
    public void postSkipChatEvent(ChatBoxScreen chatBoxScreen, ResourceLocation resourceLocation, String group, Integer index) {
        SkipChatEvent.EVENT.invoker().skipChat(chatBoxScreen, resourceLocation, group, index);
    }
}
