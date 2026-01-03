package com.zhenshiz.chatbox.neoforge.client;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.render.ChatBoxRender;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = ChatBox.MOD_ID, value = Dist.CLIENT)
public class ClientNeoForge {

    @SubscribeEvent
    public static void ChatBoxRenderEvent(RenderGuiEvent.Pre event) {
        ChatBoxRender.onHudRender(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void ChatBoxRenderTick(ClientTickEvent.Post event) {
        ChatBoxRender.onEndTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void keyInput(InputEvent.Key event) {
        ChatBoxRender.onKey(event.getKey(), event.getScanCode(), event.getAction(), event.getModifiers());
    }

    @SubscribeEvent
    public static void mouseInput(InputEvent.MouseButton.Post event) {
        ChatBoxRender.mousePost(event.getButton(), event.getAction(), event.getModifiers());
    }

    @SubscribeEvent
    public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
        if (ChatBoxRender.onMouseScroll(event.getScrollDeltaX(), event.getScrollDeltaY(), event.isLeftDown(), event.isMiddleDown(), event.isRightDown(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }
}
