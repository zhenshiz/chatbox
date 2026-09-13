package com.zhenshiz.chatbox.event;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.command.ChatBoxCommand;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.data.ChatBoxSavedData;
import com.zhenshiz.chatbox.data.ChatBoxThemeLoader;
import com.zhenshiz.chatbox.network.s2c.ChatBoxPayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
//? fabric {
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import com.zhenshiz.chatbox.event.fabric.SkipChatEvent;
//?}
//? forge {
/*import com.zhenshiz.chatbox.event.forge.SkipChatEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
*///?}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//? forge
/*@Mod.EventBusSubscriber(modid = ChatBox.MOD_ID)*/
public class ChatBoxServerEvents {

    //? fabric {
    private static class ThemeFabric extends ChatBoxThemeLoader implements IdentifiableResourceReloadListener {
        public ResourceLocation getFabricId() {return ChatBox.id("chatbox/theme");}
    }

    private static class DialoguesFabric extends ChatBoxDialoguesLoader implements IdentifiableResourceReloadListener {
        public ResourceLocation getFabricId() {return ChatBox.id("chatbox/dialogues");}
    }

    private static void loadSavedData(MinecraftServer server, ServerLevel world) {
        //只需要保存在主世界的data目录下即可
        if (world.dimension() == Level.OVERWORLD)
            //? >= 1.21 {
            ChatBox.setSavedData(world.getDataStorage().computeIfAbsent(ChatBoxSavedData.factory(world), "chatbox_saved_data"));
            //?} else {
            /*ChatBox.setSavedData(world.getDataStorage().computeIfAbsent(nbt -> ChatBoxSavedData.fromNbt(world, nbt), () -> new ChatBoxSavedData(world), "chatbox_saved_data"));*///?}
    }

    public static void init() {
        ResourceManagerHelper resourceManagerHelper = ResourceManagerHelper.get(PackType.SERVER_DATA);
        resourceManagerHelper.registerReloadListener(new ThemeFabric());
        resourceManagerHelper.registerReloadListener(new DialoguesFabric());
        CommandRegistrationCallback.EVENT.register(ChatBoxCommand::register);
        ServerWorldEvents.LOAD.register(ChatBoxServerEvents::loadSavedData);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((s, manager, bl) -> {
            //? >= 1.21
            ChatBoxDialoguesLoader.loadCriteria(s);
            s.getPlayerList().getPlayers().forEach(ChatBoxServerEvents::initializeChatBoxScreen);
        });
        //? >= 1.21
        ServerLifecycleEvents.SERVER_STARTED.register(ChatBoxDialoguesLoader::loadCriteria);
        SkipChatEvent.EVENT.register(ChatBoxServerEvents::onSkipChat);
    }//?} else {

    /*@SubscribeEvent
    public static void chatBoxLoader(AddReloadListenerEvent event) {
        event.addListener(new ChatBoxThemeLoader());
        event.addListener(new ChatBoxDialoguesLoader());
    }

    @SubscribeEvent
    public static void initializeChatBoxScreen(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        //发包到客户端
        if (player != null) initializeChatBoxScreen(player);
        else event.getPlayerList().getPlayers().forEach(ChatBoxServerEvents::initializeChatBoxScreen);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ChatBoxCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        var levelAccessor = event.getLevel();
        //只需要保存在主世界的data目录下即可
        if (levelAccessor instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) {
            ChatBox.setSavedData(world.getDataStorage().computeIfAbsent(nbt -> ChatBoxSavedData.fromNbt(world, nbt), () -> new ChatBoxSavedData(world), "chatbox_saved_data"));
        }
    }

    @SubscribeEvent
    public static void onSkipChat(SkipChatEvent event) {
        onSkipChat(event.getPlayer(), event.getResourceLocation(), event.getGroup(), event.getIndex(), event.getTargets());
    }
    *///?}

    public static void initializeChatBoxScreen(ServerPlayer player) {
        //玩家进入以及重载数据包后，发包到客户端
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.ChatBoxDataToClient("theme", cutString(ChatBoxThemeLoader.themeMap)));
        ChatBox.PLATFORM.sendToClient(player, new ChatBoxPayload.ChatBoxDataToClient("dialogues", cutString(ChatBoxDialoguesLoader.dialoguesMap)));
    }

    //由于字符串长度的限制为32767，所以需要把字符串分割成多个字符串，然后再发送给客户端
    private static final int STRING_SIZE_LIMIT = 32000;
    private static Map<ResourceLocation, List<String>> cutString(Map<ResourceLocation, String> map) {
        Map<ResourceLocation, List<String>> result = new HashMap<>();
        for (var entry : map.entrySet()) {
            ResourceLocation rl = entry.getKey();
            String data = entry.getValue();
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < data.length(); i += STRING_SIZE_LIMIT) {
                parts.add(data.substring(i, Math.min(i + STRING_SIZE_LIMIT, data.length())));
            }
            result.put(rl, parts);
        }
        return result;
    }

    private static void onSkipChat(Player p, ResourceLocation rl, String group, int index, List<Entity> targets) {
        if (!(p instanceof ServerPlayer player)) return;
        var data = ChatBox.getSavedData();
        if (!data.isPlayerLeader(player)) return;
        for (var member : data.getMembers(player)) {
            if (member == player) continue;
            ChatBoxCommandUtil.serverSkipDialogues(member, rl, group, index, targets);
            ChatBoxCommandUtil.setBlockInput(member, index != -1);
        }
    }
}
