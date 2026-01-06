package com.zhenshiz.chatbox.mixin.client;

//? fabric
import com.zhenshiz.chatbox.event.fabric.InputEvent;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    //? fabric {
    @Inject(method = "keyPress", at = @At("TAIL"))
    public void keyPress(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        InputEvent.KEY.invoker().onKey(key, scanCode, action, modifiers);
    }
    //?}
}
