package com.zhenshiz.chatbox.platform;

import com.zhenshiz.chatbox.network.CustomPacket;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    File getGameDirectory();

    void runOnClient(Runnable runnable);

    void sendToServer(CustomPacket packet);

    void sendToClient(ServerPlayer player, CustomPacket packet);

    boolean postRenderEventPre(GuiGraphics guiGraphics);

    void postRenderEventPost(GuiGraphics guiGraphics);

    void postSkipChatEvent(ChatBoxScreen chatBoxScreen, ResourceLocation resourceLocation, String group, Integer index);
}
