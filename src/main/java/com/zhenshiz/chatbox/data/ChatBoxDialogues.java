package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChatBoxDialogues {
    private final static Boolean DEFAULT_BOOL = false;

    public DialogBox dialogBox;
    public List<String> portrait;
    public List<Option> options;
    public String sound;
    public Float volume;
    public Float pitch;

    public static class DialogBox {
        public String name;
        public String text;
        public Boolean isTranslatable;

        public ResourceLocation dialoguesResourceLocation;
        public String group;
        public Integer index;

        public com.zhenshiz.chatbox.component.DialogBox setDialogBoxDialogues(com.zhenshiz.chatbox.component.DialogBox dialogBox) {
            return setDialogBoxDialogues(dialogBox, this.index);
        }

        public com.zhenshiz.chatbox.component.DialogBox setDialogBoxDialogues(com.zhenshiz.chatbox.component.DialogBox dialogBox, int index) {
            this.index = index;
            return dialogBox.setName(this.name, this.isTranslatable)
                    .setText(this.text, this.isTranslatable)
                    .setDialoguesInfo(this.dialoguesResourceLocation, this.group, index)
                    .resetTickCount();
        }
    }

    public static class Option {
        public String text;
        public Boolean isTranslatable;
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

        public static List<ChatOption> setChatOptionDialogues(ChatBoxTheme theme, ResourceLocation dialoguesResourceLocation, String group, int index) {
            List<ChatBoxDialogues> chatBoxDialogues = ChatBoxUtil.dialoguesMap.get(dialoguesResourceLocation).get(group);
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerScoreboard scoreboard = null;
            if (server != null) {
                List<ChatOption> chatOptions = new ArrayList<>();
                if (index >= 0 && index < chatBoxDialogues.size()) {
                    ChatBoxDialogues chatBoxDialogue = chatBoxDialogues.get(index);
                    int i = -1;
                    ChatBoxTheme.Option option = theme.option;
                    for (Option value : chatBoxDialogue.options) {
                        scoreboard = server.getScoreboard();
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
                        ChatOption chatOption = new ChatOption().setOptionTooltip(value.tooltip, value.isTranslatable)
                                .setOptionChat(value.text, value.isTranslatable)
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

    public static List<com.zhenshiz.chatbox.component.Portrait> setPortraitDialogues(List<String> portraits, ChatBoxTheme theme) {
        Map<String, ChatBoxTheme.Portrait> map = theme.portrait;

        List<com.zhenshiz.chatbox.component.Portrait> portraitList = new ArrayList<>();
        if (map != null && !map.isEmpty()) {
            portraits.forEach(p -> {
                com.zhenshiz.chatbox.component.Portrait portrait = map.get(p)
                        .setPortraitTheme()
                        .build();
                if (!CollUtil.isEmpty(portrait.customAnimation)) {
                    portrait.setIsAnimation(true).setTarget(portrait.x, portrait.y, getValueOrDefault(portrait.scale, 1f), portrait.opacity);
                    if (portrait.loop)
                        portrait.setStart(portrait.x, portrait.y, getValueOrDefault(portrait.scale, 1f), portrait.opacity);
                } else if (portrait.type.equals(Portrait.Type.TEXTURE) && !portrait.animationType.equals(Portrait.AnimationType.CUSTOM)) {
                    portrait.setIsAnimation(true).setTarget();
                }
                portraitList.add(portrait);
            });
        }
        return portraitList;
    }

    public void setDefaultValue(ResourceLocation resourceLocation, String group, int index) {
        this.sound = getValueOrDefault(this.sound, "");
        this.volume = getValueOrDefault(this.volume, 1f);
        this.pitch = getValueOrDefault(this.pitch, 1f);

        this.dialogBox.isTranslatable = getValueOrDefault(this.dialogBox.isTranslatable, DEFAULT_BOOL);
        this.dialogBox.dialoguesResourceLocation = resourceLocation;
        this.dialogBox.group = group;
        this.dialogBox.index = index;

        if (!CollUtil.isEmpty(this.options)) {
            for (Option option : this.options) {
                option.isTranslatable = getValueOrDefault(option.isTranslatable, DEFAULT_BOOL);
                option.isLock = getValueOrDefault(option.isLock, DEFAULT_BOOL);
                option.isHidden = getValueOrDefault(option.isHidden, DEFAULT_BOOL);
                option.dialoguesResourceLocation = resourceLocation;
                option.group = group;
                option.index = index;
            }
        }
    }

    private static <T> T getValueOrDefault(T param, T defaultValue) {
        return Optional.ofNullable(param).orElse(defaultValue);
    }
}
