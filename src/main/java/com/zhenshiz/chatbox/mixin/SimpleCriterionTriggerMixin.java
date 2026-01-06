package com.zhenshiz.chatbox.mixin;

import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
//? < 1.21
/*import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;*/
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(SimpleCriterionTrigger.class)
//? >= 1.21
public class SimpleCriterionTriggerMixin<T extends SimpleCriterionTrigger.SimpleInstance> {
//? < 1.21
/*public class SimpleCriterionTriggerMixin<T extends AbstractCriterionTriggerInstance> {*/

    @Inject(method = "trigger", at = @At("TAIL"))
    protected void trigger(ServerPlayer player, Predicate<T> testTrigger, CallbackInfo ci) {
        ChatBoxDialoguesLoader.triggerDialog(player, testTrigger);
    }
}
