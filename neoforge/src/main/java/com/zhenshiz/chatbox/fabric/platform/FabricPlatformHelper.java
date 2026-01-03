package com.zhenshiz.chatbox.fabric.platform;

import com.zhenshiz.chatbox.fabric.event.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.fabric.event.SkipChatEvent;
import com.zhenshiz.chatbox.platform.IPlatformHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.List;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public boolean isModLoaded(String modId) {return FabricLoader.getInstance().isModLoaded(modId);}

    @Override
    public boolean isDevelopmentEnvironment() {return FabricLoader.getInstance().isDevelopmentEnvironment();}

    @Override
    public File getGameDirectory() {return FabricLoader.getInstance().getGameDir().toFile();}

    @Override
    public void sendToServer(CustomPacketPayload packet) {ClientPlayNetworking.send(packet);}

    @Override
    public void sendToClient(ServerPlayer player, CustomPacketPayload packet) {ServerPlayNetworking.send(player, packet);}

    @Override
    public boolean postRenderEventPre(GuiGraphics guiGraphics) {
        return ChatBoxRenderEvent.PRE.invoker().pre(guiGraphics);
    }

    @Override
    public void postRenderEventPost(GuiGraphics guiGraphics) {
        ChatBoxRenderEvent.POST.invoker().post(guiGraphics);
    }

    @Override
    public void postSkipChatEvent(Player player, Identifier identifier, String group, Integer index, List<Entity> targets) {
        SkipChatEvent.EVENT.invoker().skipChat(player, identifier, group, index, targets);
    }
}
