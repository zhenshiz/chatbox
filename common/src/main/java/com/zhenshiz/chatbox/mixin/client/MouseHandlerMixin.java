package com.zhenshiz.chatbox.mixin.client;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    //我真的服了mojang的神奇代码，为什么screen没了还要调用afterMouseAction()
    @Redirect(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;afterMouseAction()V"))
    private void onScroll(Screen instance) {
        if (instance != null) instance.afterMouseAction();
    }
}
