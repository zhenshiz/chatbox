package com.zhenshiz.chatbox.network;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.ComponentEvent;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.event.neoforge.SkipChatEvent;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil.*;

public record SimplePayload(String name, String value) implements CustomPacketPayload {
    public static final Type<SimplePayload> TYPE = new Type<>(ChatBox.id("simple_payload"));
    public static final StreamCodec<FriendlyByteBuf, SimplePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SimplePayload::name,
            ByteBufCodecs.STRING_UTF8,
            SimplePayload::value,
            SimplePayload::new
    );

    public static final String REQUEST_SYNC         = "request_sync";
    public static final String SKIP_CHAT_C2S        = "skip_chat_c2s";

    public static final String SKIP_CHAT_S2C        = "skip_chat_s2c";
    public static final String OPEN_DIALOG          = "open_dialog";
    public static final String SET_THEME            = "set_theme";
    public static final String NEXT_DIALOGUE        = "next_dialogue";
    public static final String AUTO_PLAY            = "auto_play";
    public static final String SET_IS_SCREEN        = "set_is_screen";
    public static final String SET_DIALOG_BOX       = "set_dialog_box";
    public static final String ADD_CHAT_OPTION      = "add_chat_option";
    public static final String SET_CHAT_OPTION      = "set_chat_option";
    public static final String CLEAR_CHAT_OPTION    = "clear_chat_option";

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

    public static void handleOnClient(SimplePayload payload, IPayloadContext context) {
        String name = payload.name();
        String value = payload.value();
        if (handlersS2C.containsKey(name)) {
            handlersS2C.get(name).accept(value);
        }
    }

    public static void handleOnServer(ServerPlayer player, SimplePayload packet) {
        if (handlersC2S.containsKey(packet.name))
            player.server.execute(() -> handlersC2S.get(packet.name).accept(player, packet.value));
    }

    public static void simplePayloadS2C(ServerPlayer player, String name, String value) {
        player.connection.send(new SimplePayload(name, value));
    }

    public static void simplePayloadC2S(String name, String value) {
        PacketDistributor.sendToServer(new SimplePayload(name, value));
    }

    static {
        addHandlerC2S(REQUEST_SYNC, (player, s) -> serverSyncEntityData(player));
        addHandlerC2S(SKIP_CHAT_C2S, (player, s) -> {
            String[] parsed = StrUtil.parse(s);
            if (parsed.length != 3) return;
            onPlayerSkipChat(player, ChatBox.parseId(parsed[0]), parsed[1], Integer.parseInt(parsed[2]));
        });

        addHandlerS2C(SKIP_CHAT_S2C, s -> {
            String[] parsed = StrUtil.parse(s);
            if (parsed.length != 3) return;
            clientSkipDialogues(ChatBox.parseId(parsed[0]), parsed[1], Integer.parseInt(parsed[2]));
        });
        addHandlerS2C(OPEN_DIALOG, s -> clientOpenChatBox());
        addHandlerS2C(SET_THEME, ChatBoxCommandUtil::clientToggleTheme);
        addHandlerS2C(NEXT_DIALOGUE, s -> clientNextDialogue());
        addHandlerS2C(AUTO_PLAY, s -> clientAutoPlay(Boolean.parseBoolean(s)));
        addHandlerS2C(SET_IS_SCREEN, s -> clientSetIsScreen(Boolean.parseBoolean(s)));
        addHandlerS2C(SET_DIALOG_BOX, s -> {
            String[] parts = StrUtil.parse(s);
            if (parts.length != 2) return;
            clientSetDialogBox(parts[0], parts[1]);
        });
        addHandlerS2C(ADD_CHAT_OPTION, s -> {
            String[] parts = StrUtil.parse(s);
            if (parts.length != 5) return;
            clientAddChatOption(parts[0], parts[1], parts[2], parts[3], parts[4]);
        });
        addHandlerS2C(SET_CHAT_OPTION, s -> {
            String[] parts = StrUtil.parse(s);
            if (parts.length != 5) return;
            clientSetChatOption(Integer.parseInt(parts[0]), parts[1], parts[2], Boolean.parseBoolean(parts[3]), Boolean.parseBoolean(parts[4]));
        });
        addHandlerS2C(CLEAR_CHAT_OPTION, s -> clientClearChatOption());
    }

    private static void onPlayerSkipChat(ServerPlayer player, ResourceLocation rl, String group, int index) {
        if (index != -1) {
            var dialog = ChatBoxDialoguesLoader.parsedDialogues.get(rl).dialogues.get(group).get(index);
            if (dialog.command != null) ComponentEvent.executeCommands(player, dialog.command);

            var options = dialog.options;
            if (options != null) for (var option : options) {
                String parsedText = null; String parsedTip = null; Boolean bl = null;
                var text = option.text;
                if (text != null && !text.isEmpty()) {
                    parsedText = parsePlaceholders(player, text);
                    if (parsedText.equals(text)) parsedText = null;
                }
                var tip = option.tooltip;
                if (tip != null && !tip.isEmpty()) {
                    parsedTip = parsePlaceholders(player, tip);
                    if (parsedTip.equals(tip)) parsedTip = null;
                }
                var unlockCommand = option.unlockCommand; // 解锁命令测试通过后，取消锁定和隐藏
                if (unlockCommand != null && unlockCommand.startsWith("execute") && ComponentEvent.executeCommand(player.server, player, unlockCommand) == 1) bl = false;
                if (parsedText != null || parsedTip != null || bl != null)
                    serverSetChatOption(player, options.indexOf(option), parsedText, parsedTip, bl, bl);
            }

            var dialogBox = dialog.dialogBox;
            String parsedName = null; String parsedText = null;
            var name = dialogBox.name; var text = dialogBox.text;
            if (!name.isEmpty()) {
                parsedName = parsePlaceholders(player, name);
                if (parsedName.equals(name)) parsedName = null;
            }
            if (!text.isEmpty()) {
                parsedText = parsePlaceholders(player, text);
                if (parsedText.equals(text)) parsedText = null;
            }
            if (parsedName != null || parsedText != null) serverSetDialogBox(player, parsedName, parsedText);
        }
        NeoForge.EVENT_BUS.post(new SkipChatEvent(player, rl, group, index, serverGetChatTargets(player)));
    }

    public static void execute(SimplePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player instanceof ServerPlayer) {
            handleOnServer((ServerPlayer) player, payload);
        } else {
            handleOnClient(payload, context);
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
