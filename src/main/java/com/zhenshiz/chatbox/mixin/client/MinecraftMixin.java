package com.zhenshiz.chatbox.mixin.client;

import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.render.ChatBoxRender;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "pauseGame", at = @At(value = "HEAD"), cancellable = true)
    private void pauseGame(CallbackInfo ci) {
        if (ChatBoxRender.isRenderChatBox() && ChatBoxUtil.chatBoxScreen.isEsc) {
            ChatBoxRender.onClose();
            ci.cancel();
        }
    }

    //? forge {
    /*@Inject(method = "setScreen", at = @At(value = "HEAD"), cancellable = true)
    private void setScreen(Screen screen, CallbackInfo ci) {
        if (screen == null) return;
        // 懒得添加依赖，用这种方式阻止吧
        if (screen.getClass().getName().equals("org.confluence.terraentity.client.gui.container.DialogScreen")
                || screen.getClass().getName().equals("org.confluence.terraentity.client.gui.container.AnglerDialogScreen")) {
            if (ChatBoxClient.conf.isStopTerraDialog) ci.cancel();
        }
    }
    *///?}
}
