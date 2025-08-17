package com.zhenshiz.chatbox.render;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChatBox.MOD_ID, value = Dist.CLIENT)
public class ChatBoxRender {

    @SubscribeEvent
    public static void ChatBoxRenderEvent(RenderGuiEvent.Pre event) {
        ChatBoxRenderCommon.onHudRender(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void ChatBoxRenderTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ChatBoxRenderCommon.onEndTick(Minecraft.getInstance());
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
        if (ChatBoxRenderCommon.onMouseScroll(event.getScrollDelta(), event.isLeftDown(), event.isMiddleDown(), event.isRightDown(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }
}
