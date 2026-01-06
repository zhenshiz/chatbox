package com.zhenshiz.chatbox.event;

//? forge {
/*import com.zhenshiz.chatbox.event.forge.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.event.forge.SkipChatEvent;
import com.zhenshiz.chatbox.event.kubejs.ChatBoxRenderEventJS;
import com.zhenshiz.chatbox.event.kubejs.SkipChatEventJS;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CommonEventsPostJS {

    public static class Client {

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void chatBoxRenderPre(ChatBoxRenderEvent.Pre event) {
            if (ChatBoxEventsJS.CHAT_BOX_RENDER_PRE.hasListeners()) {
                EventResult result = ChatBoxEventsJS.CHAT_BOX_RENDER_PRE.post(new ChatBoxRenderEventJS.Pre(event));
                if (result.interruptFalse()) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void chatBoxRenderPost(ChatBoxRenderEvent.Post event) {
            if (ChatBoxEventsJS.CHAT_BOX_RENDER_POST.hasListeners()) {
                ChatBoxEventsJS.CHAT_BOX_RENDER_POST.post(new ChatBoxRenderEventJS.Post(event));
            }
        }
    }

    public static class Common {
        @SubscribeEvent(priority = EventPriority.LOW)
        public static void chatBoxSkipChat(SkipChatEvent event) {
            if (ChatBoxEventsJS.CHAT_BOX_SKIP_CHAT.hasListeners()) {
                if (event.getPlayer() instanceof ServerPlayer) {
                    ChatBoxEventsJS.CHAT_BOX_SKIP_CHAT.post(ScriptType.SERVER, new SkipChatEventJS(event));
                } else {
                    ChatBoxEventsJS.CHAT_BOX_SKIP_CHAT.post(ScriptType.CLIENT, new SkipChatEventJS(event));
                }
            }
        }
    }
}
*/