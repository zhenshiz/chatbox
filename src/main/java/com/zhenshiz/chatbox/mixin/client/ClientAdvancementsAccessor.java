package com.zhenshiz.chatbox.mixin.client;

//? < 1.21
/*import net.minecraft.advancements.Advancement;*/
//? >= 1.21
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ClientAdvancements.class)
public interface ClientAdvancementsAccessor {

    @Accessor("progress")
    //? < 1.21
    /*Map<Advancement, AdvancementProgress> getProgress();*/
    //? >= 1.21
    Map<AdvancementHolder, AdvancementProgress> getProgress();
}
