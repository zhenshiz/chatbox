package com.zhenshiz.chatbox.fabric.mixin.client;

import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(BuiltInPackSource.class)
public class BuiltInPackSourceMixin {

    @Inject(method = "loadPacks", at = @At(value = "RETURN"))
    public void loadPacks(Consumer<Pack> result, CallbackInfo ci) {
        if ((Object) this instanceof ClientPackSource) com.zhenshiz.chatbox.editor.EditorPreviewPack.register(result);
    }
}
