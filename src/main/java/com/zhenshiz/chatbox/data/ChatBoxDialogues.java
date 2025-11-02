package com.zhenshiz.chatbox.data;

import com.google.gson.JsonElement;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatBoxDialogues {
    public Map<String, List<Dialogues>> dialogues = new HashMap<>();
    public Boolean isTranslatable = false;
    public Boolean isEsc = true;
    public Boolean isPause = true;
    public Boolean isHistoricalSkip = true;
    public Integer maxTriggerCount = -1;
    public Boolean isScreen;
    public String theme;

    public static class Dialogues {
        public DialogBox dialogBox = new DialogBox();
        public List<JsonElement> portrait;
        public List<Option> options;
        public String sound = "";
        public Float volume = 1f;
        public Float pitch = 1f;
        public String command;
        public String backgroundImage;
        public Video video;
        public Boolean clearOldPortrait = true;
        public List<String> removePortrait;

        public List<Portrait> setPortraitDialogues(List<Portrait> portraitList) {
            Map<String, ChatBoxTheme.Portrait> map = ChatBoxUtil.chatBoxTheme.portrait;
            var animations = ChatBoxUtil.animationMap;
            if (clearOldPortrait) portraitList.clear();
            else if (!CollUtil.isEmpty(removePortrait)) {
                portraitList.removeIf(portrait -> removePortrait.contains(portrait.id));
            }
            if (map != null && !map.isEmpty()) {
                parsePortrait().forEach(p -> {
                    Portrait portrait = null;
                    if (p instanceof String s) {
                        try {
                            portrait = map.get(s)
                                    .setPortraitTheme()
                                    .build();
                            portrait.id = s;
                        } catch (Exception e) {
                            ChatBox.LOGGER.error("portrait {} not found", p);
                        }
                    } else if (p instanceof ReplacePortrait replacePortrait) {
                        try {
                            portrait = replacePortrait.replace(map.get(replacePortrait.id))
                                    .setPortraitTheme()
                                    .build();
                            portrait.id = replacePortrait.id;
                        } catch (Exception e) {
                            ChatBox.LOGGER.error("portrait {} not found", replacePortrait.id);
                        }
                    }

                    if (portrait != null) {
                        List<ChatBoxTheme.Portrait.CustomAnimation> animation = new ArrayList<>();
                        // 先看立绘是否有动画类型字段
                        if (portrait.animationType != null && animations.containsKey(portrait.animationType))
                            animation = animations.get(portrait.animationType);
                        // 如果还定义了自定义动画，就用自定义动画覆盖（两种动画是不可以同时生效的）
                        if (!CollUtil.isEmpty(portrait.customAnimation)) animation = portrait.customAnimation;
                        if (!CollUtil.isEmpty(animation)) {
                            portrait.setCustomAnimation(animation);
                            portrait.setIsAnimation(true).setTarget(portrait.x, portrait.y, portrait.scale, portrait.opacity, portrait.angle);
                            if (portrait.loop)
                                portrait.setStart(portrait.x, portrait.y, portrait.scale, portrait.opacity, portrait.angle);
                        }
                        portraitList.add(portrait);
                    }
                });
            }
            return portraitList;
        }

        private List<Object> parsePortrait() {
            List<Object> portraitList = new ArrayList<>();
            if (CollUtil.isEmpty(portrait)) return portraitList;
            for (JsonElement element : portrait) {
                if (element.isJsonPrimitive()) {
                    portraitList.add(element.getAsString());
                } else if (element.isJsonObject()) {
                    ReplacePortrait obj = ChatBoxUtil.GSON.fromJson(element, ReplacePortrait.class);
                    portraitList.add(obj);
                }
            }
            return portraitList;
        }

        public static class DialogBox {
            public String name = "";
            public String text = "";

            public com.zhenshiz.chatbox.component.DialogBox setDialogBoxDialogues(com.zhenshiz.chatbox.component.DialogBox dialogBox, boolean isTranslatable) {
                return dialogBox.setName(this.name, isTranslatable)
                        .setText(this.text, isTranslatable)
                        .resetTickCount()
                        .setAllOver(false);
            }
        }

        public static class ReplacePortrait extends ChatBoxTheme.Portrait {
            { // 给Component直接设置初始值唯一的缺点
                x = null;
                y = null;
                width = null;
                height = null;
                alignX = null;
                alignY = null;
                opacity = null;
                renderOrder = null;
                angle = null;
                scale = null;
                loop = null;
            }

            public String id;

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
                Path gameDir = FMLPaths.GAMEDIR.get().normalize().toAbsolutePath();
                File file = new File(gameDir.toString(), path);
                if (!file.exists()) file = new File(path);
                if (!file.exists()) {
                    ChatBox.LOGGER.error("video {} not found", path);
                    return null;
                }
                return new com.zhenshiz.chatbox.component.Video(file.toURI(), canControl, canSkip, loop)
                        .setDefaultOption(x, y, width, height, AbstractComponent.AlignX.of(alignX), AbstractComponent.AlignY.of(alignY), opacity, renderOrder, angle);
            }
        }

        public static class Option {
            public String text;
            public Boolean isLock = false;
            public Boolean isHidden = false;
            public String unlockCommand;
            public String next;
            public Click click = new Click();
            public String tooltip;

            public static class Click {
                public String type;
                public String value;
            }
        }

        public List<ChatOption> setChatOptionDialogues(boolean isTranslatable) {
            List<ChatOption> chatOptions = new ArrayList<>();
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && !CollUtil.isEmpty(this.options)) {
                for (Option option : this.options) {
                    ChatOption chatOption = new ChatOption().setOptionTooltip(option.tooltip, isTranslatable)
                            .setOptionChat(option.text, isTranslatable)
                            .setIsLock(option.isLock)
                            .setUnlockCommand(option.unlockCommand)
                            .setNext(option.next)
                            .setClickEvent(option.click.type, option.click.value);
                    chatOptions.add(ChatBoxUtil.chatBoxTheme.option.setChatOptionTheme(chatOption));
                }
            }
            return chatOptions;
        }
    }
}
