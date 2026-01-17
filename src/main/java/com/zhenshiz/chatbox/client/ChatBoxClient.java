package com.zhenshiz.chatbox.client;

//? fabric {
import com.zhenshiz.chatbox.Config;
import com.zhenshiz.chatbox.event.fabric.InputEvent;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.render.ChatBoxRender;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class ChatBoxClient implements ClientModInitializer {
    public static Config conf;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(Config.class, Toml4jConfigSerializer::new);
        conf = AutoConfig.getConfigHolder(Config.class).getConfig();
        registerReceiver();
        registerRenderEvents();
    }

    private static void registerReceiver() {
        //? >= 1.21 {
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.ChatBoxDataToClient.TYPE, ChatBoxPayload.ChatBoxDataToClient::execute);
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.SyncEntityData.TYPE, ChatBoxPayload.SyncEntityData::execute);
        ClientPlayNetworking.registerGlobalReceiver(SimplePayload.TYPE, SimplePayload::execute);
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.ChatBoxDataToClient.ID, (client, h, buf, r) -> ChatBoxPayload.ChatBoxDataToClient.handleOnClient(ChatBoxPayload.ChatBoxDataToClient.decode(buf)));
        ClientPlayNetworking.registerGlobalReceiver(ChatBoxPayload.SyncEntityData.ID, (client, h, buf, r) -> ChatBoxPayload.SyncEntityData.handleOnClient(ChatBoxPayload.SyncEntityData.decode(buf)));
        ClientPlayNetworking.registerGlobalReceiver(SimplePayload.ID, (client, h, buf, r) -> SimplePayload.handleOnClient(SimplePayload.decode(buf)));
        *///?}
    }

    private void registerRenderEvents() {
        HudRenderCallback.EVENT.register(ChatBoxRender::onHudRender);
        ClientTickEvents.END_CLIENT_TICK.register(ChatBoxRender::onEndTick);
        InputEvent.KEY.register(ChatBoxRender::onKey);
        InputEvent.MouseButton.POST.register(ChatBoxRender::mousePost);
        InputEvent.MOUSE_SCROLLING.register(ChatBoxRender::onMouseScroll);
    }
}
//?}

//? neoforge {
//?}

//? forge {
/*import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.Config;
import com.zhenshiz.chatbox.render.ChatBoxRender;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChatBox.MOD_ID, value = Dist.CLIENT)
public class ChatBoxClient {
    public static Config conf;

    public static void init() {
        AutoConfig.register(Config.class, Toml4jConfigSerializer::new);
        conf = AutoConfig.getConfigHolder(Config.class).getConfig();
    }

    @SubscribeEvent
    public static void ChatBoxRenderEvent(RenderGuiEvent.Pre event) {
        ChatBoxRender.onHudRender(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void ChatBoxRenderTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ChatBoxRender.onEndTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void keyInput(InputEvent.Key event) {
        ChatBoxRender.onKey(event.getKey(), event.getScanCode(), event.getAction(), event.getModifiers());
    }

    @SubscribeEvent
    public static void mouseButton(InputEvent.MouseButton.Post event) {
        ChatBoxRender.mousePost(event.getButton(), event.getAction(), event.getModifiers());
    }

    @SubscribeEvent
    public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
        if (ChatBoxRender.onMouseScroll(0, event.getScrollDelta(), event.isLeftDown(), event.isMiddleDown(), event.isRightDown(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }
}
*///?}
