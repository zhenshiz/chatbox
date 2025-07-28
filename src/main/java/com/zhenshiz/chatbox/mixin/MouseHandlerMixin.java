package com.zhenshiz.chatbox.mixin;

import com.zhenshiz.chatbox.render.ChatBoxRender;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Redirect(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;afterMouseAction()V"))
    private void onScroll(Screen instance) {
        if (instance != null) instance.afterMouseAction();
    }

    @Inject(method = "onScroll",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;swapPaint(D)V"), cancellable = true)
    private void onInventorySwapPaint(long windowPointer, double xOffset, double yOffset, CallbackInfo ci){
        if (ChatBoxRender.isOpenChatBox){
            ci.cancel();
        }
    }
}
