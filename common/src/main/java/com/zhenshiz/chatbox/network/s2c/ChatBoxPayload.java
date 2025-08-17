package com.zhenshiz.chatbox.network.s2c;

import com.zhenshiz.chatbox.network.CustomPacket;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zhenshiz.chatbox.ChatBox.PLATFORM;
import static com.zhenshiz.chatbox.ChatBox.ResourceLocationMod;

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
            PLATFORM.runOnClient(() -> ChatBoxUtil.skipDialogues(packet.dialogues, packet.group, packet.index));
        }
    }

    public record OpenChatBox() implements CustomPacket {
        public ResourceLocation id() {return ID;}
        public static final ResourceLocation ID = ResourceLocationMod("open_dialog");

        public void write(FriendlyByteBuf buf) {}

        public static void encode(OpenChatBox packet, FriendlyByteBuf buf) {}

        public static OpenChatBox decode(FriendlyByteBuf buf) {return new OpenChatBox();}

        public static void handleOnClient(OpenChatBox packet) {
            PLATFORM.runOnClient(ChatBoxCommandUtil::clientOpenChatBox);
        }
    }

    public record ToggleTheme(ResourceLocation theme) implements CustomPacket {
        public ResourceLocation id() {return ID;}
        public static final ResourceLocation ID = ResourceLocationMod("toggle_theme");

        public void write(FriendlyByteBuf buf) {encode(this, buf);}

        public static void encode(ToggleTheme packet, FriendlyByteBuf buf) {buf.writeResourceLocation(packet.theme);}

        public static ToggleTheme decode(FriendlyByteBuf buf) {return new ToggleTheme(buf.readResourceLocation());}

        public static void handleOnClient(ToggleTheme packet) {
            PLATFORM.runOnClient(() -> ChatBoxCommandUtil.clientToggleTheme(packet.theme));
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
                if (ChatBoxCommandUtil.themeResourceLocation != null) {
                    ResourceLocation theme = ResourceLocation.tryParse(ChatBoxCommandUtil.themeResourceLocation);
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
