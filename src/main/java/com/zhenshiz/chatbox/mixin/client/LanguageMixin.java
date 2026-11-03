package com.zhenshiz.chatbox.mixin.client;

import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Language.class)
public class LanguageMixin {

    @Inject(method = "inject", at = @At("RETURN"))
    private static void inject(Language instance, CallbackInfo ci) {
        com.zhenshiz.chatbox.component.KeyPromptRender.onLanguageChanged();
    }
}
