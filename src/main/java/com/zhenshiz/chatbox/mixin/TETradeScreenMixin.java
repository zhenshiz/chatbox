package com.zhenshiz.chatbox.mixin;

import com.zhenshiz.chatbox.Config;
import org.confluence.terraentity.client.gui.container.TETradeScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TETradeScreen.class)
public class TETradeScreenMixin {
    @Shadow
    boolean triggerOnce;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void TETradeScreen(CallbackInfo ci) {
        this.triggerOnce = !Config.isStopTerraDialog.get();
    }
}
