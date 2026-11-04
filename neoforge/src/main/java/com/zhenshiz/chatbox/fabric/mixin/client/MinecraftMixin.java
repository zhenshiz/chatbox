package com.zhenshiz.chatbox.fabric.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", ordinal = 1))
    private void renderFramePost(boolean advanceGameTime, CallbackInfo ci) {
        com.zhenshiz.chatbox.editor.EditorBridgeClient.renderFrame();
    }
}
