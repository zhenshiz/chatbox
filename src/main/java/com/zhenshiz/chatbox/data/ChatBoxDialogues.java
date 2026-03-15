package com.zhenshiz.chatbox.data;

import com.google.gson.JsonElement;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatBoxDialogues {
    public Map<String, List<Dialogues>> dialogues = new HashMap<>();
    public Boolean isEsc = true;
    public Boolean isPause = true;
    public Boolean isHistoricalSkip = true;
    public Integer maxTriggerCount = -1;
    public Boolean isScreen;
    public String theme;
    public float animationFPS = 60F;
    public int autoPlayTick = 20;
    public JsonElement criteria;

    public static class Dialogues {
        public DialogBox dialogBox = new DialogBox();
        public List<JsonElement> portrait;
        public List<Option> options;
        public String sound = "";
        public String command;
        public String backgroundImage;
        public Video video;
        public Boolean clearOldPortrait = true;
        public List<String> removePortrait;
        public List<ChatBoxTheme.RenderEvent> renderEvents;

        public List<Portrait<?>> setPortraitDialogues(List<Portrait<?>> portraitList) {
            if (clearOldPortrait) portraitList.clear();
            else if (!CollUtil.isEmpty(removePortrait))
                portraitList.removeIf(portrait -> removePortrait.contains(portrait.id));

            if (CollUtil.notEmpty(portrait)) for (JsonElement jsonElement : portrait) {
                Object o = getValue(jsonElement);
                if (o == null) continue;
                var portraits = ChatBoxUtil.chatBoxTheme.portrait;

                Portrait<?> portrait = null;
                if (o instanceof String s && !s.isEmpty()) {
                    try {
                        portrait = portraits.get(s).setPortraitTheme().setId(s);
                    } catch (Exception e) {
                        ChatBox.LOGGER.error("Portrait {} not found", s);
                    }
                } else if (o instanceof ReplacePortrait rp) {
                    try {
                        portrait = rp.replace(portraits.get(rp.id)).setPortraitTheme().setId(rp.id);
                        if (rp.replace) portraitList.removeIf(p -> p.id.equals(rp.id));
                    } catch (Exception e) {
                        ChatBox.LOGGER.error("Portrait {} not found", rp.id);
                    }
                }
                if (portrait != null) portraitList.add(portrait);
            }
            return portraitList;
        }

        private static Object getValue(JsonElement value) {
            try {
                if (value.isJsonPrimitive()) return value.getAsString();
                if (value.isJsonObject()) return ChatBoxUtil.GSON.fromJson(value, ReplacePortrait.class);
            } catch (Exception ignored) {
            }
            return null;
        }

        public static class DialogBox {
            public String name = "";
            public String text = "";

            public com.zhenshiz.chatbox.component.DialogBox setDialogBoxDialogues(com.zhenshiz.chatbox.component.DialogBox dialogBox) {
                return dialogBox.setName(this.name).setText(this.text)
                        .setAllOver(false);
            }
        }

        public static class ReplacePortrait extends ChatBoxTheme.Portrait {
            { // 给Component直接设置初始值唯一的缺点
                renderOrder = null;
            }
            public String id;
            public boolean replace = false;

            public ChatBoxTheme.Portrait replace(ChatBoxTheme.Portrait portrait) {
                ChatBoxTheme.Portrait copy = new ChatBoxTheme.Portrait();
                BeanUtil.copyProperties(portrait, copy);
                BeanUtil.copyProperties(this, copy);
                return copy;
            }
        }

        public static class Video extends ChatBoxTheme.Component {
            {
                this.x = 0f;
                this.y = 0f;
                this.width = 100f;
                this.height = 100f;
                this.renderOrder = -1;
            }
            public String path;
            public Boolean canControl = true;
            public Boolean canSkip = true;
            public Boolean loop = false;

            public com.zhenshiz.chatbox.component.Video setVideo() {
                if (!ChatBox.isWaterMediaLoaded()) return null;
                URI uri;
                File file = new File(FMLPaths.GAMEDIR.get().toFile(), path);
                if (!file.exists()) file = new File(path);
                try {
                    if (file.exists()) uri = file.toURI();
                    else uri = new URI(path);
                } catch (URISyntaxException e) {
                    return null;
                }
                return new com.zhenshiz.chatbox.component.Video(uri, canControl, canSkip, loop).of(this);
            }
        }

        public static class Option {
            public String text;
            public Boolean isLock = false;
            public String unlockCommand;
            public String next;
            public Click click = new Click();
            public String tooltip;

            public static class Click {
                public String type;
                public String value;
            }
        }

        public List<ChatOption> setChatOptionDialogues() {
            List<ChatOption> chatOptions = new ArrayList<>();
            if (CollUtil.notEmpty(options)) for (Option option : this.options) {
                ChatOption chatOption = ChatBoxUtil.chatBoxTheme.option.newOption().setOptionTooltip(option.tooltip)
                        .setOptionChat(option.text)
                        .setNext(option.next)
                        .setClickEvent(option.click.type, option.click.value)
                        .setCondition(option.unlockCommand, option.isLock);
                chatOptions.add(chatOption);
            }
            return chatOptions;
        }
    }
}
