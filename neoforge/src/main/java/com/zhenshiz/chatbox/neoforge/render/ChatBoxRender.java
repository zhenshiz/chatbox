package com.zhenshiz.chatbox.neoforge.render;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.render.ChatBoxRenderCommon;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = ChatBox.MOD_ID, value = Dist.CLIENT)
public class ChatBoxRender {

    @SubscribeEvent
    public static void ChatBoxRenderEvent(RenderGuiEvent.Pre event) {
        ChatBoxRenderCommon.onHudRender(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void ChatBoxRenderTick(ClientTickEvent.Post event) {
        ChatBoxRenderCommon.onEndTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void ChatBoxRenderKeyInput(InputEvent.Key event) {
        ChatBoxRenderCommon.onKey(event.getKey(), event.getScanCode(), event.getAction(), event.getModifiers());
    }

    @SubscribeEvent
    public static void ChatBoxRenderMouseInput(InputEvent.MouseButton.Post event) {
        ChatBoxRenderCommon.mousePost(event.getButton(), event.getAction(), event.getModifiers());
    }

    @SubscribeEvent
    public static void ChatBoxRenderKeyInput(InputEvent.MouseScrollingEvent event) {
        if (ChatBoxRenderCommon.onMouseScroll(event.getScrollDeltaX(), event.getScrollDeltaY(), event.isLeftDown(), event.isMiddleDown(), event.isRightDown(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }
}
