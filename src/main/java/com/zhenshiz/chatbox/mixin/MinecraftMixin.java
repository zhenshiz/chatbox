package com.zhenshiz.chatbox.mixin;

import com.zhenshiz.chatbox.client.ChatBoxRender;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "pauseGame", at = @At(value = "HEAD"), cancellable = true)
    private void pauseGame(CallbackInfo ci) {
        if (ChatBoxRender.isRenderChatBox() && ChatBoxUtil.chatBoxScreen.isEsc) {
            ChatBoxUtil.closeDialogBox();
            ci.cancel();
        }
    }
}
