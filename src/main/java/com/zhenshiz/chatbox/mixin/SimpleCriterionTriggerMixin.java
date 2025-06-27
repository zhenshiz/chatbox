package com.zhenshiz.chatbox.mixin;

import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(SimpleCriterionTrigger.class)
public class SimpleCriterionTriggerMixin<T extends SimpleCriterionTrigger.SimpleInstance> {

    @Inject(method = "trigger", at = @At("TAIL"))
    protected void trigger(ServerPlayer player, Predicate<T> testTrigger, CallbackInfo ci) {
        ChatBoxDialoguesLoader.triggerDialog(player, testTrigger);
    }
}