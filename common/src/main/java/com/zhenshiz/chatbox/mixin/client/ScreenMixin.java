package com.zhenshiz.chatbox.mixin.client;

import com.zhenshiz.chatbox.render.ChatBoxRenderCommon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    public void renderBackground(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (ChatBoxRenderCommon.isRenderChatBox()) ci.cancel();
    }
}
