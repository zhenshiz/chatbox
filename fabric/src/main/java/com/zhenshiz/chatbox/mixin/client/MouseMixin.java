package com.zhenshiz.chatbox.mixin.client;

import com.zhenshiz.chatbox.event.fabric.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {

    @Shadow @Final private Minecraft minecraft;

    @Shadow private boolean isLeftPressed;
    @Shadow private boolean isMiddlePressed;
    @Shadow private boolean isRightPressed;
    @Shadow private double xpos;
    @Shadow private double ypos;

    @Inject(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", ordinal = 0), cancellable = true)
    private void mousePre(long windowPointer, int button, int action, int modifiers, CallbackInfo ci) {
        if (InputEvent.MouseButton.PRE.invoker().mousePre(button, action, modifiers)) {
            ci.cancel();
        }
    }

    @Inject(method = "onPress", at = @At(value = "TAIL"))
    private void mousePost(long windowPointer, int button, int action, int modifiers, CallbackInfo ci) {
        if (windowPointer == this.minecraft.getWindow().getWindow()) InputEvent.MouseButton.POST.invoker().mousePost(button, action, modifiers);
    }

    @Inject(method = "onScroll", at= @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"), cancellable = true)
    private void onScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci){
        if (InputEvent.MOUSE_SCROLLING.invoker().onMouseScroll(yOffset, isLeftPressed, isMiddlePressed, isRightPressed, xpos, ypos)) {
            ci.cancel();
        }
    }
}
