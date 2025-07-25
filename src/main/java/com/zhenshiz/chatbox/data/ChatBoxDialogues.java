package com.zhenshiz.chatbox.data;

import com.google.gson.JsonElement;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatBoxDialogues {
    private final static Boolean DEFAULT_BOOL = false;

    public Map<String, List<Dialogues>> dialogues = new HashMap<>();
    public Boolean isTranslatable;
    public Boolean isEsc;
    public Boolean isPause;
    public Boolean isHistoricalSkip;
    public String theme;

    public void setDefaultValue(ResourceLocation resourceLocation) {
        for (Map.Entry<String, List<Dialogues>> entry : dialogues.entrySet()) {
            int index = 0;
            entry.getValue().forEach(dialogues -> {
                dialogues.setDefaultValue(resourceLocation, entry.getKey(), index);
            });
        }

        this.isTranslatable = BeanUtil.getValueOrDefault(this.isTranslatable, false);
        this.isEsc = BeanUtil.getValueOrDefault(this.isEsc, true);
        this.isPause = BeanUtil.getValueOrDefault(this.isPause, true);
        this.isHistoricalSkip = BeanUtil.getValueOrDefault(this.isHistoricalSkip, true);
    }

    public static class Dialogues {
        public DialogBox dialogBox;
        public List<JsonElement> portrait;
        public List<Option> options;
        public String sound;
        public Float volume;
        public Float pitch;
        public String command;
        public String backgroundImage;
        public Video video;

        public static List<Portrait> setPortraitDialogues(List<Object> portraits, ChatBoxTheme theme) {
            Map<String, ChatBoxTheme.Portrait> map = theme.portrait;

            List<Portrait> portraitList = new ArrayList<>();
            if (map != null && !map.isEmpty()) {
                portraits.forEach(p -> {
                    Portrait portrait = null;
                    if (p instanceof String) {
                        try {
                            portrait = map.get(p)
                                  .setPortraitTheme()
                                  .build();
                        } catch (Exception e) {
                            ChatBox.LOGGER.error("portrait {} not found", p);
                        }
                    } else if (p instanceof ReplacePortrait replacePortrait) {
                        try {
                            portrait = replacePortrait.replace(map.get(replacePortrait.id))
                                  .setPortraitTheme()
                                  .build();
                        } catch (Exception e) {
                            ChatBox.LOGGER.error("portrait {} not found", replacePortrait.id);
                        }
                    }

                    if (portrait != null) {
                        if (!CollUtil.isEmpty(portrait.customAnimation)) {
                            BeanUtil.copyPropertiesIfTargetNull(new ChatBoxTheme.Portrait.CustomAnimation(portrait.x, portrait.y, BeanUtil.getValueOrDefault(portrait.scale, 1f), portrait.opacity), portrait.customAnimation.getFirst());
                            if (portrait.customAnimation.size() > 1) {
                                for (int i = 1; i < portrait.customAnimation.size(); i++) {
                                    BeanUtil.copyPropertiesIfTargetNull(portrait.customAnimation.get(i - 1), portrait.customAnimation.get(i));
                                }
                            }
                            portrait.setIsAnimation(true).setTarget(portrait.x, portrait.y, BeanUtil.getValueOrDefault(portrait.scale, 1f), portrait.opacity);
                            if (portrait.loop)
                                portrait.setStart(portrait.x, portrait.y, BeanUtil.getValueOrDefault(portrait.scale, 1f), portrait.opacity);
                        } else if (portrait.type.equals(Portrait.Type.TEXTURE) && !portrait.animationType.equals(Portrait.AnimationType.CUSTOM)) {
                            portrait.setIsAnimation(true).setTarget();
                        }
                        portraitList.add(portrait);
                    }
                });
            }
            return portraitList;
        }

        public static List<Object> parsePortrait(List<JsonElement> jsonElements) {
            List<Object> portraitList = new ArrayList<>();
            for (JsonElement element : jsonElements) {
                if (element.isJsonPrimitive()) {
                    portraitList.add(element.getAsString());
                } else if (element.isJsonObject()) {
                    ReplacePortrait obj = ChatBoxUtil.GSON.fromJson(element, ReplacePortrait.class);
                    portraitList.add(obj);
                }
            }
            return portraitList;
        }

        public void setDefaultValue(ResourceLocation resourceLocation, String group, int index) {
            this.sound = BeanUtil.getValueOrDefault(this.sound, "");
            this.volume = BeanUtil.getValueOrDefault(this.volume, 1f);
            this.pitch = BeanUtil.getValueOrDefault(this.pitch, 1f);

            this.dialogBox.dialoguesResourceLocation = resourceLocation;
            this.dialogBox.group = group;
            this.dialogBox.index = index;

            if (!CollUtil.isEmpty(this.options)) {
                for (Option option : this.options) {
                    option.isLock = BeanUtil.getValueOrDefault(option.isLock, DEFAULT_BOOL);
                    option.isHidden = BeanUtil.getValueOrDefault(option.isHidden, DEFAULT_BOOL);
                    option.dialoguesResourceLocation = resourceLocation;
                    option.group = group;
                    option.index = index;
                }
            }
        }

        public static class DialogBox {
            public String name;
            public String text;

            public ResourceLocation dialoguesResourceLocation;
            public String group;
            public Integer index;

            public com.zhenshiz.chatbox.component.DialogBox setDialogBoxDialogues(com.zhenshiz.chatbox.component.DialogBox dialogBox, int index, boolean isTranslatable) {
                this.index = index;
                return dialogBox.setName(this.name, isTranslatable)
                        .setText(this.text, isTranslatable)
                        .setDialoguesInfo(this.dialoguesResourceLocation, this.group, index)
                        .resetTickCount();
            }
        }

        public static class ReplacePortrait extends ChatBoxTheme.Portrait {
            public String id;

            public ChatBoxTheme.Portrait replace(ChatBoxTheme.Portrait portrait) {
                ChatBoxTheme.Portrait copy = new ChatBoxTheme.Portrait();
                BeanUtil.copyProperties(portrait, copy);
                BeanUtil.copyProperties(this, copy);
                return copy;
            }
        }

        public static class Video extends ChatBoxTheme.Component {
            public String path;
            public Boolean canControl;
            public Boolean canSkip;
            public Boolean loop;

            public void setDefaultValue() {
                this.x = BeanUtil.getValueOrDefault(this.x, 0f);
                this.y = BeanUtil.getValueOrDefault(this.y, 0f);
                this.width = BeanUtil.getValueOrDefault(this.width, 100f);
                this.height = BeanUtil.getValueOrDefault(this.height, 100f);
                this.alignX = BeanUtil.getValueOrDefault(this.alignX, AbstractComponent.AlignX.LEFT.name());
                this.alignY = BeanUtil.getValueOrDefault(this.alignY, AbstractComponent.AlignY.TOP.name());
                this.opacity = BeanUtil.getValueOrDefault(this.opacity, 100f);
                this.renderOrder = BeanUtil.getValueOrDefault(this.renderOrder, -1);

                this.canControl = BeanUtil.getValueOrDefault(this.canControl, true);
                this.canSkip = BeanUtil.getValueOrDefault(this.canSkip, true);
                this.loop = BeanUtil.getValueOrDefault(this.loop, false);
            }

            public com.zhenshiz.chatbox.component.Video setVideo() {
                if (!ChatBox.isWaterMediaLoaded()) return null;
                setDefaultValue();
                Path gameDir = FabricLoader.getInstance().getGameDir();
                File file = new File(gameDir.toString(), path);
                if (!file.exists()) file = new File(path);
                if (!file.exists()) {
                    ChatBox.LOGGER.error("video {} not found", path);
                    return null;
                }
                return new com.zhenshiz.chatbox.component.Video(file.toURI(), canControl, canSkip, loop)
                        .setDefaultOption(x, y, width, height, AbstractComponent.AlignX.of(alignX), AbstractComponent.AlignY.of(alignY), opacity, renderOrder);
            }
        }

        public static class Option {
            public String text;
            public Boolean isLock;
            public Condition lock = new Condition();
            public Condition hidden = new Condition();
            public Boolean isHidden;
            public String next;
            public Click click = new Click();
            public String tooltip;

            public ResourceLocation dialoguesResourceLocation;
            public String group;
            public Integer index;

            public static List<ChatOption> setChatOptionDialogues(ChatBoxTheme theme, ResourceLocation dialoguesResourceLocation, String group, int index, boolean isTranslatable) {
                List<Dialogues> chatBoxDialogues = ChatBoxUtil.dialoguesMap.get(dialoguesResourceLocation).dialogues.get(group);
                ClientLevel level = Minecraft.getInstance().level;
                Scoreboard scoreboard;
                if (level != null) {
                    List<ChatOption> chatOptions = new ArrayList<>();
                    if (index >= 0 && index < chatBoxDialogues.size()) {
                        Dialogues dialog = chatBoxDialogues.get(index);
                        int i = -1;
                        ChatBoxTheme.Option option = theme.option;
                        for (Option value : dialog.options) {
                            scoreboard = level.getScoreboard();
                            Objective objective = scoreboard.getObjective(value.hidden.objective);
                            ScoreAccess scoreAccess = null;
                            if (objective != null) {
                                scoreAccess = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(value.hidden.value), objective);
                            }
                            //如果这个选项标记隐藏，那么如果对应的计分板不在或者计分板的值不为1则隐藏这个选项
                            if (value.isHidden && (scoreAccess == null || scoreAccess.get() != 1)) {
                                continue;
                            }
                            i++;
                            objective = scoreboard.getObjective(value.lock.objective);
                            if (objective != null) {
                                scoreAccess = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(value.lock.value), objective);
                            }
                            ChatOption chatOption = new ChatOption().setOptionTooltip(value.tooltip, isTranslatable)
                                    .setOptionChat(value.text, isTranslatable)
                                    //如果这个选项标记上锁，那么如果对应的计分板不在或者计分板的值不为1则给这个选项上锁
                                    .setIsLock(value.isLock && (scoreAccess == null || scoreAccess.get() != 1))
                                    .setNext(value.next)
                                    .setClickEvent(value.click.type, value.click.value)
                                    .setDialoguesInfo(dialoguesResourceLocation, group, index);

                            chatOptions.add(option.setChatOptionTheme(chatOption, i));
                        }
                    }
                    return chatOptions;
                }
                return new ArrayList<>();
            }

            public static class Click {
                public String type;
                public String value;
            }

            public static class Condition {
                public String objective;
                public String value;
            }
        }
    }
}
