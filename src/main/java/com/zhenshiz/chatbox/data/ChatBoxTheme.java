package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.utils.math.EasingUtil;

import java.util.Map;
import java.util.Optional;

public class ChatBoxTheme {
    private final static Integer DEFAULT_INT = 0;

    public Map<String, Portrait> portrait;
    public Option option;
    public DialogBox dialogBox;
    public LogButton logButton;

    public static class Portrait extends Component {
        public String value;
        public Integer opacity;
        public Integer customItemData;
        public String animation;
        public Integer duration;
        public String easing;
        public Integer scale;

        public com.zhenshiz.chatbox.component.Portrait setPortraitTheme(com.zhenshiz.chatbox.component.Portrait portrait) {
            switch (portrait.type) {
                case TEXTURE ->
                        portrait.createTexture(portrait, this.value, this.opacity, this.animation, this.easing, this.duration).build();
                case PLAYER_HEAD -> portrait.createPlayerHead(portrait, this.value).build();
                case ITEM -> portrait.createItem(portrait, this.value, this.customItemData, this.scale).build();
            }
            return portrait.setDefaultOption(this.x, this.y, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.z);
        }
    }

    public static class Option extends Component {
        public String texture;
        public String selectTexture;
        public String lockTexture;
        public Integer optionChatX;
        public Integer optionChatY;
        public String textAlign;

        public ChatOption setChatOptionTheme(ChatOption chatOption, int index) {
            return chatOption.setDefaultOption(this.x, this.y + this.height * index, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.z)
                    .setTextures(this.texture)
                    .setSelectTexture(this.selectTexture)
                    .setLockTexture(this.lockTexture)
                    .setOptionChatPosition(this.optionChatX, this.optionChatY)
                    .setTextAlign(ChatOption.TextAlign.of(this.textAlign))
                    .build();
        }
    }

    public static class DialogBox extends Component {
        public String texture;
        public Integer lineWidth;
        public Integer nameX;
        public Integer nameY;
        public Integer textX;
        public Integer textY;

        public com.zhenshiz.chatbox.component.DialogBox setDialogBoxTheme(com.zhenshiz.chatbox.component.DialogBox dialogBox) {
            return dialogBox.setDefaultOption(this.x, this.y, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.z)
                    .setTexture(this.texture)
                    .setNamePosition(this.nameX, this.nameY)
                    .setTextPosition(this.textX, this.textY)
                    .setLineWidth(this.lineWidth)
                    .build();
        }
    }

    public static class LogButton extends Component {
        public String texture;
        public String hoverTexture;

        public com.zhenshiz.chatbox.component.LogButton setLogButtonTheme(com.zhenshiz.chatbox.component.LogButton logButton) {
            return logButton.setDefaultOption(this.x, this.y, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.z)
                    .setLogTexture(this.texture)
                    .setHoverLogTexture(this.hoverTexture)
                    .build();
        }
    }

    public static class Component {
        public Integer x;
        public Integer y;
        public Integer width;
        public Integer height;
        public Integer z;
        public String alignX;
        public String alignY;

        public void setDefaultValue() {
            this.x = getValueOrDefault(this.x, DEFAULT_INT);
            this.y = getValueOrDefault(this.y, DEFAULT_INT);
            this.width = getValueOrDefault(this.width, 10);
            this.height = getValueOrDefault(this.height, 10);
            this.z = getValueOrDefault(this.z, DEFAULT_INT);
            this.alignX = getValueOrDefault(this.alignX, AbstractComponent.AlignX.LEFT.name());
            this.alignY = getValueOrDefault(this.alignY, AbstractComponent.AlignY.TOP.name());
        }
    }

    public ChatBoxTheme(Map<String, Portrait> portrait, Option option, DialogBox dialogBox, LogButton logButton) {
        this.portrait = portrait;
        this.option = option;
        this.dialogBox = dialogBox;
        this.logButton = logButton;
    }

    public ChatBoxTheme setDefaultValue() {
        this.portrait.forEach((key, value) -> {
            value.opacity = getValueOrDefault(value.opacity, 100);
            value.animation = getValueOrDefault(value.animation, com.zhenshiz.chatbox.component.Portrait.AnimationType.NONE.name());
            value.easing = getValueOrDefault(value.easing, EasingUtil.Easing.EASE_IN_SINE.name());
            value.scale = getValueOrDefault(value.scale, 1);
            value.setDefaultValue();
        });

        this.option.optionChatX = getValueOrDefault(this.option.optionChatX, DEFAULT_INT);
        this.option.optionChatY = getValueOrDefault(this.option.optionChatY, DEFAULT_INT);
        this.option.textAlign = getValueOrDefault(this.option.textAlign, ChatOption.TextAlign.LEFT.name());
        this.option.setDefaultValue();

        this.dialogBox.nameX = getValueOrDefault(this.dialogBox.nameX, DEFAULT_INT);
        this.dialogBox.nameY = getValueOrDefault(this.dialogBox.nameY, DEFAULT_INT);
        this.dialogBox.textX = getValueOrDefault(this.dialogBox.textX, DEFAULT_INT);
        this.dialogBox.textY = getValueOrDefault(this.dialogBox.textY, DEFAULT_INT);
        this.dialogBox.setDefaultValue();

        this.logButton.setDefaultValue();

        return this;
    }

    private static <T> T getValueOrDefault(T param, T defaultValue) {
        return Optional.ofNullable(param).orElse(defaultValue);
    }
}
