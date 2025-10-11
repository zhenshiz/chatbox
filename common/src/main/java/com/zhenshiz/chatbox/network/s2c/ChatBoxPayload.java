package com.zhenshiz.chatbox.network.s2c;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.network.CustomPacket;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.zhenshiz.chatbox.ChatBox.PLATFORM;
import static com.zhenshiz.chatbox.ChatBox.ResourceLocationMod;

@SuppressWarnings("unused")
public class ChatBoxPayload {

    public record OpenScreen(ResourceLocation dialogues, String group, int index) implements CustomPacket {
        public ResourceLocation id() {return ID;}
        public static final ResourceLocation ID = ResourceLocationMod("open_screen");

        public void write(FriendlyByteBuf buf) {encode(this, buf);}

        public static void encode(OpenScreen packet, FriendlyByteBuf buf) {
            buf.writeResourceLocation(packet.dialogues);
            buf.writeUtf(packet.group);
            buf.writeInt(packet.index);
        }

        public static OpenScreen decode(FriendlyByteBuf buf) {
            return new OpenScreen(buf.readResourceLocation(), buf.readUtf(), buf.readInt());
        }

        public static void handleOnClient(OpenScreen packet) {
            PLATFORM.runOnClient(() -> ChatBoxCommandUtil.clientSkipDialogues(packet.dialogues, packet.group, packet.index));
        }
    }

    public record AllChatBoxThemeToClient(Map<ResourceLocation, List<String>> themeMap) implements CustomPacket {
        public ResourceLocation id() {return ID;}
        public static final ResourceLocation ID = ResourceLocationMod("all_chat_box_theme_to_client");

        public void write(FriendlyByteBuf buf) {encode(this, buf);}

        public static void encode(AllChatBoxThemeToClient packet, FriendlyByteBuf buf) {
            buf.writeMap(packet.themeMap, FriendlyByteBuf::writeResourceLocation, (vBuf, v) -> vBuf.writeCollection(v, FriendlyByteBuf::writeUtf));
        }

        public static AllChatBoxThemeToClient decode(FriendlyByteBuf buf) {
            return new AllChatBoxThemeToClient(buf.readMap(FriendlyByteBuf::readResourceLocation, v -> v.readList(FriendlyByteBuf::readUtf)));
        }

        public static void handleOnClient(AllChatBoxThemeToClient packet) {
            PLATFORM.runOnClient(() -> {
                ChatBoxUtil.setTheme(mergeString(packet.themeMap));
                if (ChatBoxUtil.themeResourceLocation != null) {
                    ResourceLocation theme = ResourceLocation.tryParse(ChatBoxUtil.themeResourceLocation);
                    if (theme != null) {
                        ChatBoxUtil.toggleTheme(theme);
                    }
                }
            });
        }
    }

    public record AllChatBoxDialoguesToClient(Map<ResourceLocation, List<String>> dialoguesMap) implements CustomPacket {
        public ResourceLocation id() {return ID;}
        public static final ResourceLocation ID = ResourceLocationMod("all_chat_box_dialogues_to_client");

        public void write(FriendlyByteBuf buf) {encode(this, buf);}

        public static void encode(AllChatBoxDialoguesToClient packet, FriendlyByteBuf buf) {
            buf.writeMap(packet.dialoguesMap, FriendlyByteBuf::writeResourceLocation, (vBuf, v) -> vBuf.writeCollection(v, FriendlyByteBuf::writeUtf));
        }

        public static AllChatBoxDialoguesToClient decode(FriendlyByteBuf buf) {
            return new AllChatBoxDialoguesToClient(buf.readMap(FriendlyByteBuf::readResourceLocation, v -> v.readList(FriendlyByteBuf::readUtf)));
        }

        public static void handleOnClient(AllChatBoxDialoguesToClient packet) {
            PLATFORM.runOnClient(() -> ChatBoxUtil.setDialogues(mergeString(packet.dialoguesMap)));
        }
    }

    public record SimplePayload(String name, String value) implements CustomPacket {
        public ResourceLocation id() {return ID;}
        public static final ResourceLocation ID = ChatBox.ResourceLocationMod("simple_payload");

        public void write(FriendlyByteBuf buf) {encode(this, buf);}

        public static void encode(SimplePayload packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.name); buf.writeUtf(packet.value);
        }

        public static SimplePayload decode(FriendlyByteBuf buf) {
            return new SimplePayload(buf.readUtf(), buf.readUtf());
        }

        private static final Map<String, Consumer<String>> handlers = new HashMap<>();
        static {
            handlers.put("open_dialog", s -> ChatBoxCommandUtil.clientOpenChatBox());
            handlers.put("set_theme", ChatBoxCommandUtil::clientToggleTheme);
            handlers.put("next_dialogue", s -> ChatBoxCommandUtil.clientNextDialogue());
            handlers.put("auto_play", s -> ChatBoxCommandUtil.clientAutoPlay(Boolean.parseBoolean(s)));
            handlers.put("set_is_screen", s -> ChatBoxCommandUtil.clientSetIsScreen(Boolean.parseBoolean(s)));
        }

        public static void handleOnClient(SimplePayload packet) {
            PLATFORM.runOnClient(() -> {
                if (handlers.containsKey(packet.name)) handlers.get(packet.name).accept(packet.value);
            });
        }
    }

    private static Map<ResourceLocation, String> mergeString(Map<ResourceLocation, List<String>> map) {
        Map<ResourceLocation, String> result = new HashMap<>();
        for (var entry : map.entrySet()) {
            ResourceLocation rl = entry.getKey();
            List<String> parts = entry.getValue();
            StringBuilder builder = new StringBuilder();
            for (String part : parts) {
                builder.append(part);
            }
            result.put(rl, builder.toString());
        }
        return result;
    }
}
