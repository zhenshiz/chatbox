package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.render.KeyPromptRender;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import com.zhenshiz.chatbox.utils.math.EasingUtil;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class ChatBoxTheme {
    private final static Float DEFAULT_FLOAT = 0F;

    public Map<String, Portrait> portrait;
    public Option option;
    public DialogBox dialogBox;
    public LogButton logButton;
    public KeyPrompt keyPrompt;

    public ChatBoxTheme setDefaultValue() {
        this.portrait.forEach((key, value) -> {
            value.opacity = BeanUtil.getValueOrDefault(value.opacity, 100f);
            value.animation = BeanUtil.getValueOrDefault(value.animation, com.zhenshiz.chatbox.component.Portrait.AnimationType.NONE.name());
            value.easing = BeanUtil.getValueOrDefault(value.easing, EasingUtil.Easing.EASE_IN_SINE.name());
            value.scale = BeanUtil.getValueOrDefault(value.scale, 1f);
            value.setDefaultValue();
            value.renderOrder = BeanUtil.getValueOrDefault(value.renderOrder, 20);
            value.loop = BeanUtil.getValueOrDefault(value.loop, false);
        });

        this.option.optionChatX = BeanUtil.getValueOrDefault(this.option.optionChatX, DEFAULT_FLOAT);
        this.option.optionChatY = BeanUtil.getValueOrDefault(this.option.optionChatY, DEFAULT_FLOAT);
        this.option.textAlign = BeanUtil.getValueOrDefault(this.option.textAlign, ChatOption.TextAlign.LEFT.name());
        this.option.setDefaultValue();
        this.option.renderOrder = BeanUtil.getValueOrDefault(this.option.renderOrder, 10);

        this.dialogBox.nameX = BeanUtil.getValueOrDefault(this.dialogBox.nameX, DEFAULT_FLOAT);
        this.dialogBox.nameY = BeanUtil.getValueOrDefault(this.dialogBox.nameY, DEFAULT_FLOAT);
        this.dialogBox.textX = BeanUtil.getValueOrDefault(this.dialogBox.textX, DEFAULT_FLOAT);
        this.dialogBox.textY = BeanUtil.getValueOrDefault(this.dialogBox.textY, DEFAULT_FLOAT);
        this.dialogBox.setDefaultValue();
        this.dialogBox.renderOrder = BeanUtil.getValueOrDefault(this.dialogBox.renderOrder, 0);

        this.logButton.setDefaultValue();
        this.logButton.renderOrder = BeanUtil.getValueOrDefault(this.logButton.renderOrder, 30);

        this.keyPrompt.setDefaultValue();
        this.keyPrompt.visible = BeanUtil.getValueOrDefault(this.keyPrompt.visible, true);
        this.keyPrompt.renderOrder = BeanUtil.getValueOrDefault(this.keyPrompt.renderOrder, 40);

        return this;
    }

    public static class Portrait extends Component {
        public String type;
        public String value;
        public Integer customItemData;
        public String animation;
        public Integer duration;
        public String easing;
        public Float scale;
        public List<CustomAnimation> customAnimation = new ArrayList<>();
        public Boolean loop;

        public com.zhenshiz.chatbox.component.Portrait setPortraitTheme() {
            com.zhenshiz.chatbox.component.Portrait portrait = new com.zhenshiz.chatbox.component.Portrait(com.zhenshiz.chatbox.component.Portrait.Type.of(this.type), this.customAnimation, this.loop, this.scale);
            switch (portrait.type) {
                case TEXTURE ->
                        portrait.createTexture(portrait, this.value, this.animation, this.easing, this.duration).build();
                case PLAYER_HEAD -> portrait.createPlayerHead(portrait, this.value).build();
                case ITEM -> portrait.createItem(portrait, this.value, this.customItemData).build();
            }
            return portrait.setDefaultOption(this.x, this.y, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.opacity, this.renderOrder);
        }

        public static class CustomAnimation {
            public String texture;
            public Integer time;
            public Float x;
            public Float y;
            public Float scale;
            public Float opacity;
            public EasingUtil.Easing easing;

            public CustomAnimation() {
            }

            public CustomAnimation(Float x, Float y, Float scale, Float opacity) {
                this.x = x;
                this.y = y;
                this.scale = scale;
                this.opacity = opacity;
            }
        }
    }

    public static class Option extends Component {
        public String texture;
        public String selectTexture;
        public String lockTexture;
        public Float optionChatX;
        public Float optionChatY;
        public String textAlign;

        public ChatOption setChatOptionTheme(ChatOption chatOption, int index) {
            return chatOption.setDefaultOption(this.x, this.y + this.height * index, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.opacity, this.renderOrder)
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
        public Float lineWidth;
        public Float nameX;
        public Float nameY;
        public Float textX;
        public Float textY;

        public com.zhenshiz.chatbox.component.DialogBox setDialogBoxTheme(com.zhenshiz.chatbox.component.DialogBox dialogBox) {
            return dialogBox.setDefaultOption(this.x, this.y, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.opacity, this.renderOrder)
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
            return logButton.setDefaultOption(this.x, this.y, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.opacity, this.renderOrder)
                    .setLogTexture(this.texture)
                    .setHoverLogTexture(this.hoverTexture)
                    .build();
        }
    }

    public static class KeyPrompt extends Component {
        public Boolean visible;
        public String rightClickTexture;
        public String scrollTexture;

        public KeyPromptRender setKeyPromptTheme(KeyPromptRender keyPromptRender) {
            return keyPromptRender.setPosition(this.x, this.y)
                    .setAlign(AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY))
                    .setOpacity(this.opacity)
                    .setRenderOrder(this.renderOrder)
                    .setVisible(this.visible)
                    .setRightClickTexture(this.rightClickTexture)
                    .setScrollTexture(this.scrollTexture)
                    .build();
        }
    }

    public static class Component {
        public Float x;
        public Float y;
        public Float width;
        public Float height;
        public String alignX;
        public String alignY;
        public Float opacity;
        public Integer renderOrder;

        public void setDefaultValue() {
            this.x = BeanUtil.getValueOrDefault(this.x, DEFAULT_FLOAT);
            this.y = BeanUtil.getValueOrDefault(this.y, DEFAULT_FLOAT);
            this.width = BeanUtil.getValueOrDefault(this.width, 10f);
            this.height = BeanUtil.getValueOrDefault(this.height, 10f);
            this.alignX = BeanUtil.getValueOrDefault(this.alignX, AbstractComponent.AlignX.LEFT.name());
            this.alignY = BeanUtil.getValueOrDefault(this.alignY, AbstractComponent.AlignY.TOP.name());
            this.opacity = BeanUtil.getValueOrDefault(this.opacity, 100f);
        }
    }
}
