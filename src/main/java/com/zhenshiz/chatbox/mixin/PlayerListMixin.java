package com.zhenshiz.chatbox.mixin;

import com.zhenshiz.chatbox.event.ChatBoxSettingLoader;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
//? >= 1.21
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    //? fabric {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    //? >=1.21
    public void placeNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
    //? < 1.21
    /*public void placeNewPlayer(Connection netManager, ServerPlayer player, CallbackInfo ci) {*/
        ChatBoxSettingLoader.initializeChatBoxScreen(player);
    }
    //?}
}
