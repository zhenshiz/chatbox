package com.zhenshiz.chatbox.platform;

//? fabric {
import com.zhenshiz.chatbox.event.fabric.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.event.fabric.SkipChatEvent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
    //? < 1.21 {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
    *///?}
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//?}
import net.minecraft.client.Minecraft;
//? neoforge {
//?}
//? forge {
/*import com.zhenshiz.chatbox.network.Packets;
import com.zhenshiz.chatbox.event.forge.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.event.forge.SkipChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.PacketDistributor;
*///?}
//? >= 1.21
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? < 1.21
/*import com.zhenshiz.chatbox.network.CustomPacket;*/
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.List;

public interface Platform {

    Platform INSTANCE = new Platform() {};
    //? fabric {
    default boolean isModLoaded(String modId) {return FabricLoader.getInstance().isModLoaded(modId);}

    default boolean isDevelopmentEnvironment() {return FabricLoader.getInstance().isDevelopmentEnvironment();}

    default File getGameDirectory() {return FabricLoader.getInstance().getGameDir().toFile();}

    //? >= 1.21 {
    default void sendToServer(CustomPacketPayload packet) {ClientPlayNetworking.send(packet);}

    default void sendToClient(ServerPlayer player, CustomPacketPayload packet) {ServerPlayNetworking.send(player, packet);}
    //?} else {
    /*default void sendToServer(CustomPacket packet) {
        var buf = PacketByteBufs.create();
        packet.write(buf);
        ClientPlayNetworking.send(packet.id(), buf);
    }

    default void sendToClient(ServerPlayer player, CustomPacket packet) {
        var buf = PacketByteBufs.create();
        packet.write(buf);
        ServerPlayNetworking.send(player, packet.id(), buf);
    }*///?}

    default boolean postRenderEventPre(GuiGraphics guiGraphics) {
        return ChatBoxRenderEvent.PRE.invoker().pre(guiGraphics);
    }

    default void postRenderEventPost(GuiGraphics guiGraphics) {
        ChatBoxRenderEvent.POST.invoker().post(guiGraphics);
    }

    default void postSkipChatEvent(Player player, ResourceLocation identifier, String group, Integer index, List<Entity> targets) {
        SkipChatEvent.EVENT.invoker().skipChat(player, identifier, group, index, targets);
    }
    //?}

    //? < 1.21 {
    /*default void runOnClient(Runnable runnable) {
        Minecraft.getInstance().execute(runnable);
    }*///?}

    //? neoforge {
/*    default boolean isModLoaded(String modId) {return ModList.get().isLoaded(modId);}

    default boolean isDevelopmentEnvironment() {return !FMLLoader.getCurrent().isProduction();}

    default File getGameDirectory() {return FMLLoader.getCurrent().getGameDir().toFile();}

    default void sendToServer(CustomPacketPayload packet) {
        if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.connection.send(packet);
    }

    default void sendToClient(ServerPlayer player, CustomPacketPayload packet) {PacketDistributor.sendToPlayer(player, packet);}

    default boolean postRenderEventPre(GuiGraphics guiGraphics) {
        return NeoForge.EVENT_BUS.post(new ChatBoxRenderEvent.Pre(guiGraphics)).isCanceled();
    }

    default void postRenderEventPost(GuiGraphics guiGraphics) {
        NeoForge.EVENT_BUS.post(new ChatBoxRenderEvent.Post(guiGraphics));
    }

    default void postSkipChatEvent(Player player, ResourceLocation identifier, String group, Integer index, List<Entity> targets) {
        NeoForge.EVENT_BUS.post(new SkipChatEvent(player, identifier, group, index, targets));
    }*/
    //?}

    //? forge {
    /*default boolean isModLoaded(String modId) {return ModList.get().isLoaded(modId);}

    default boolean isDevelopmentEnvironment() {return !FMLLoader.isProduction();}

    default File getGameDirectory() {return FMLLoader.getGamePath().toFile();}

    default void sendToServer(CustomPacket packet) {
        Packets.getChannel(packet).sendToServer(packet);
    }

    default void sendToClient(ServerPlayer player, CustomPacket packet) {
        Packets.getChannel(packet).send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    default boolean postRenderEventPre(GuiGraphics guiGraphics) {
        return MinecraftForge.EVENT_BUS.post(new ChatBoxRenderEvent.Pre(guiGraphics));
    }

    default void postRenderEventPost(GuiGraphics guiGraphics) {
        MinecraftForge.EVENT_BUS.post(new ChatBoxRenderEvent.Post(guiGraphics));
    }

    default void postSkipChatEvent(Player player, ResourceLocation resourceLocation, String group, Integer index, List<Entity> targets) {
        MinecraftForge.EVENT_BUS.post(new SkipChatEvent(player, resourceLocation, group, index, targets));
    }
    *///?}
}
