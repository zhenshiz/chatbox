package com.zhenshiz.chatbox.network.c2s;

import com.zhenshiz.chatbox.ChatBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.confluence.terraentity.api.npc.trade.ITradeHolder;
import org.confluence.terraentity.menu.SimpleTradeMenu;
import org.confluence.terraentity.mixed.IPlayer;
import org.jetbrains.annotations.NotNull;

public record OpenTerraNpcShop() implements CustomPacketPayload {
    public static final Type<OpenTerraNpcShop> TYPE = new Type<>(ChatBox.ResourceLocationMod("open_terra_npc_shop"));
    public static final StreamCodec<FriendlyByteBuf, OpenTerraNpcShop> CODEC = StreamCodec.ofMember(OpenTerraNpcShop::write, OpenTerraNpcShop::new);

    public OpenTerraNpcShop(FriendlyByteBuf friendlyByteBuf) {
        this();
    }

    private void write(FriendlyByteBuf buf) {
    }

    public static void execute(OpenTerraNpcShop payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            if (((IPlayer) serverPlayer).terra_entity$getTradeHolder() instanceof ITradeHolder holder) {
                serverPlayer.openMenu(new SimpleMenuProvider((id, playerInventory, player2) -> new SimpleTradeMenu(id, playerInventory, holder), Component.translatable("title.terra_entity.npc_trade")));
            }
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
