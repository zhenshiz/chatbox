package com.zhenshiz.chatbox.network;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.ComponentEvent;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil.*;

public record SimplePayload(String name, String value) implements CustomPacket {
    public ResourceLocation id() {return ID;}
    public static final ResourceLocation ID = ChatBox.ResourceLocationMod("simple_payload");

    public void write(FriendlyByteBuf buf) {encode(this, buf);}

    public static void encode(SimplePayload packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.name);
        buf.writeUtf(packet.value);
    }

    public static SimplePayload decode(FriendlyByteBuf buf) {
        return new SimplePayload(buf.readUtf(), buf.readUtf());
    }

    private static final Map<String, Consumer<String>> handlersS2C = new HashMap<>();

    public static void addHandlerS2C(String name, Consumer<String> handler) {
        if (!handlersS2C.containsKey(name)) forceAddHandlerS2C(name, handler);
    }

    public static void forceAddHandlerS2C(String name, Consumer<String> handler) {
        handlersS2C.put(name, handler);
    }

    private static final Map<String, BiConsumer<ServerPlayer, String>> handlersC2S = new HashMap<>();

    public static void addHandlerC2S(String name, BiConsumer<ServerPlayer, String> handler) {
        if (!handlersC2S.containsKey(name)) forceAddHandlerC2S(name, handler);
    }

    public static void forceAddHandlerC2S(String name, BiConsumer<ServerPlayer, String> handler) {
        handlersC2S.put(name, handler);
    }

    public static void handleOnClient(SimplePayload packet) {
        ChatBox.PLATFORM.runOnClient(() -> {
            if (handlersS2C.containsKey(packet.name)) handlersS2C.get(packet.name).accept(packet.value);
        });
    }

    public static void handleOnServer(ServerPlayer player, SimplePayload packet) {
        if (handlersC2S.containsKey(packet.name))
            player.server.execute(() -> handlersC2S.get(packet.name).accept(player, packet.value));
    }

    public static final String REQUEST_SYNC         = "request_sync";
    public static final String SKIP_CHAT_C2S        = "skip_chat_c2s";
    public static final String REQUEST_UNLOCK       = "request_unlock";

    public static final String OPEN_DIALOG          = "open_dialog";
    public static final String SET_THEME            = "set_theme";
    public static final String NEXT_DIALOGUE        = "next_dialogue";
    public static final String AUTO_PLAY            = "auto_play";
    public static final String SET_IS_SCREEN        = "set_is_screen";
    public static final String SET_DIALOG_BOX       = "set_dialog_box";
    public static final String ADD_CHAT_OPTION      = "add_chat_option";
    public static final String CLEAR_CHAT_OPTION    = "clear_chat_option";
    public static final String UNLOCK_CHAT_OPTION   = "unlock_chat_option";
    public static final String HIDE_CHAT_OPTION     = "hide_chat_option";

    static {
        addSimpleHandlerC2S(REQUEST_SYNC, (player, s) -> serverSyncEntityData(player));
        addSimpleHandlerC2S(SKIP_CHAT_C2S, (player, s) -> {
            String[] parsed = StrUtil.parse(s);
            if (parsed.length != 3) return;
            ChatBox.PLATFORM.postSkipChatEvent(player, new ResourceLocation(parsed[0]), parsed[1], Integer.parseInt(parsed[2]), serverGetChatTargets(player));
        });
        addSimpleHandlerC2S(REQUEST_UNLOCK, (player, s) -> {
            String[] parsed = StrUtil.parse(s);
            if (parsed.length != 3) return;
            boolean isLock = Boolean.parseBoolean(parsed[0]);
            int result = ComponentEvent.executeCommand(player.server, player, parsed[2]);
            // 如果命令测试通过且是锁定状态，则解锁聊天选项
            if (result == 1 && isLock) simplePayloadS2C(player, UNLOCK_CHAT_OPTION, parsed[1]);
            // 如果命令测试失败且不是锁定状态，则隐藏聊天选项
            if (result != 1 && !isLock) simplePayloadS2C(player, HIDE_CHAT_OPTION, parsed[1]);
        });

        addSimpleHandlerS2C(OPEN_DIALOG, s -> clientOpenChatBox());
        addSimpleHandlerS2C(SET_THEME, ChatBoxCommandUtil::clientToggleTheme);
        addSimpleHandlerS2C(NEXT_DIALOGUE, s -> clientNextDialogue());
        addSimpleHandlerS2C(AUTO_PLAY, s -> clientAutoPlay(Boolean.parseBoolean(s)));
        addSimpleHandlerS2C(SET_IS_SCREEN, s -> clientSetIsScreen(Boolean.parseBoolean(s)));
        addSimpleHandlerS2C(SET_DIALOG_BOX, s -> {
            String[] parts = StrUtil.parse(s);
            if (parts.length != 2) return;
            clientSetDialogBox(parts[0], parts[1]);
        });
        addSimpleHandlerS2C(ADD_CHAT_OPTION, s -> {
            String[] parts = StrUtil.parse(s);
            if (parts.length != 5) return;
            clientAddChatOption(parts[0], parts[1], parts[2], parts[3], parts[4]);
        });
        addSimpleHandlerS2C(CLEAR_CHAT_OPTION, s -> clientClearChatOption());
        addSimpleHandlerS2C(UNLOCK_CHAT_OPTION, s -> clientUnlockChatOption(Integer.parseInt(s)));
        addSimpleHandlerS2C(HIDE_CHAT_OPTION, s -> clientHideChatOption(Integer.parseInt(s)));
    }
}
