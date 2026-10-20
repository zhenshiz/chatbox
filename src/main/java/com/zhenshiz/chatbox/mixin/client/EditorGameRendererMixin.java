package com.zhenshiz.chatbox.mixin.client;

import com.zhenshiz.chatbox.editor.EditorBridgeClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在世界、HUD 和界面均绘制完成后采集预览帧。 */
@Mixin(GameRenderer.class)
public abstract class EditorGameRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void chatbox$captureEditorFrame(CallbackInfo callback) {
        EditorBridgeClient.renderFrame();
    }
}
