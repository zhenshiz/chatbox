package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.utils.math.EasingUtil;

import java.util.Map;

public class ChatBoxTheme {
    private final static Integer[] DEFAULT_INT_ARRAY = new Integer[]{0, 0};
    private final static Boolean DEFAULT_BOOL = false;

    public Map<String, Portrait> portrait;
    public Option option;
    public DialogBox dialogBox;
    public LogButton logButton;

    public static class Portrait extends Component {
        public String value;
        public Integer opacity = 100;
        public Integer customItemData;
        public String animation = com.zhenshiz.chatbox.component.Portrait.AnimationType.NONE.name();
        public Integer duration;
        public String easing = EasingUtil.Easing.EASE_IN_SINE.name();

        public com.zhenshiz.chatbox.component.Portrait setPortraitTheme(com.zhenshiz.chatbox.component.Portrait portrait) {
            return portrait.setPosition(this.pos)
                    .setSize(this.size)
                    .setZ(this.z)
                    .setAlign(AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY))
                    .setValue(this.value)
                    .setOpacity(this.opacity)
                    .setCustomItemData(this.customItemData)
                    .setAnimationType(this.animation)
                    .setDurationAnimationTick(this.duration)
                    .setEasing(this.easing);
        }
    }

    public static class Option extends Component {
        public String texture;
        public String selectTexture;
        public String lockTexture;
        public Integer[] optionChatPos = DEFAULT_INT_ARRAY;
        public String textAlign;

        public ChatOption setChatOptionTheme(ChatOption chatOption, int index) {
            return chatOption.setPosition(this.pos[0], this.pos[1] + this.size[1] * index)
                    .setSize(this.size)
                    .setZ(this.z)
                    .setAlign(AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY))
                    .setTextures(this.texture)
                    .setSelectTexture(this.selectTexture)
                    .setLockTexture(this.lockTexture)
                    .setOptionChatPosition(this.optionChatPos)
                    .setTextAlign(ChatOption.TextAlign.of(this.textAlign));
        }
    }

    public static class DialogBox extends Component {
        public String texture;
        public Integer lineWidth;
        public Integer[] namePos = DEFAULT_INT_ARRAY;
        public Integer[] textPos = DEFAULT_INT_ARRAY;

        public com.zhenshiz.chatbox.component.DialogBox setDialogBoxTheme(com.zhenshiz.chatbox.component.DialogBox dialogBox) {
            return dialogBox.setPosition(this.pos)
                    .setSize(this.size)
                    .setZ(this.z)
                    .setAlign(AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY))
                    .setTexture(this.texture)
                    .setTextPosition(this.textPos)
                    .setNamePosition(this.namePos)
                    .setLineWidth(this.lineWidth);
        }
    }

    public static class LogButton extends Component {
        public String texture;
        public String hoverTexture;

        public com.zhenshiz.chatbox.component.LogButton setLogButtonTheme(com.zhenshiz.chatbox.component.LogButton logButton) {
            return logButton.setPosition(this.pos)
                    .setSize(this.size)
                    .setZ(this.z)
                    .setAlign(AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY))
                    .setLogTexture(this.texture)
                    .setHoverLogTexture(this.hoverTexture);
        }
    }

    public static class Component {
        public Integer[] pos = DEFAULT_INT_ARRAY;
        public Integer[] size = DEFAULT_INT_ARRAY;
        public Integer z;
        public String alignX;
        public String alignY;
    }

    public ChatBoxTheme(Map<String, Portrait> portrait, Option option, DialogBox dialogBox, LogButton logButton) {
        this.portrait = portrait;
        this.option = option;
        this.dialogBox = dialogBox;
        this.logButton = logButton;
    }
}
