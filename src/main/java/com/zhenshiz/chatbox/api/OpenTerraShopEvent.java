package com.zhenshiz.chatbox.api;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import org.confluence.terraentity.api.npc.trade.ITradeHolder;
import org.confluence.terraentity.entity.npc.AbstractTerraNPC;
import org.confluence.terraentity.menu.SimpleTradeMenu;
import org.confluence.terraentity.mixed.IPlayer;

public class OpenTerraShopEvent implements ChatOptionClickEvent {
    @Override
    public String getType() {
        return "TERRA_ENTITY_SHOP";
    }

    @Override
    public void executeOnClient(String value) {
    }

    @Override
    public boolean shouldExecuteOnServer() {
        ITradeHolder iTradeHolder = null;
        if (Minecraft.getInstance().player != null) {
            iTradeHolder = ((IPlayer) Minecraft.getInstance().player).terra_entity$getTradeHolder();
        }
        return iTradeHolder instanceof AbstractTerraNPC;
    }

    @Override
    public void executeOnServer(ServerPlayer player, String value) {
        if (((IPlayer) player).terra_entity$getTradeHolder() instanceof ITradeHolder holder) {
            player.openMenu(new SimpleMenuProvider((id, playerInventory, player2) -> new SimpleTradeMenu(id, playerInventory, holder), Component.translatable("title.terra_entity.npc_trade")));
        }
    }
}
