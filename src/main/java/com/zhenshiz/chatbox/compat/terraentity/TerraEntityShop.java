package com.zhenshiz.chatbox.compat.terraentity;

//? forge {
/*import com.zhenshiz.chatbox.api.EventExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import org.confluence.terraentity.api.npc.trade.ITradeHolder;
import org.confluence.terraentity.entity.npc.AbstractTerraNPC;
import org.confluence.terraentity.menu.SimpleTradeMenu;
import org.confluence.terraentity.mixed.IPlayer;

public class TerraEntityShop {

    public static void register() {
        EventExecutor.registerEvent("TERRA_ENTITY_SHOP", (c, v) -> {}, () -> {
            //noinspection ConstantConditions
            ITradeHolder iTradeHolder = ((IPlayer) Minecraft.getInstance().player).terra_entity$getTradeHolder();
            return iTradeHolder instanceof AbstractTerraNPC;
        }, (player, value) -> {
            if (((IPlayer) player).terra_entity$getTradeHolder() instanceof AbstractTerraNPC holder) {
                player.openMenu(new SimpleMenuProvider((id, playerInventory, player2) -> new SimpleTradeMenu(id, playerInventory, holder), Component.translatable("title.terra_entity.npc_trade")));
            }
        });
    }
}
*/