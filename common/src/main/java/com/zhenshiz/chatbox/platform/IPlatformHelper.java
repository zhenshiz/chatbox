package com.zhenshiz.chatbox.platform;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.List;

public interface IPlatformHelper {

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    File getGameDirectory();

    void sendToServer(CustomPacketPayload packet);

    void sendToClient(ServerPlayer player, CustomPacketPayload packet);

    boolean postRenderEventPre(GuiGraphics guiGraphics);

    void postRenderEventPost(GuiGraphics guiGraphics);

    void postSkipChatEvent(Player player, Identifier identifier, String group, Integer index, List<Entity> targets);
}
