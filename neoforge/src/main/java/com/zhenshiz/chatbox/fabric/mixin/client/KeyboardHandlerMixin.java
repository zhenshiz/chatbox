package com.zhenshiz.chatbox.fabric.mixin.client;

import com.zhenshiz.chatbox.fabric.event.InputEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("TAIL"))
    public void keyPress(long windowPointer, int action, KeyEvent keyEvent, CallbackInfo ci) {
        if (windowPointer == minecraft.getWindow().handle()) InputEvent.KEY.invoker().onKey(keyEvent.key(), keyEvent.scancode(), action, keyEvent.modifiers());
    }
}
