package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.component.FunctionalButton;
import com.zhenshiz.chatbox.render.KeyPromptRender;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import com.zhenshiz.chatbox.utils.math.EasingUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@AllArgsConstructor
public class ChatBoxTheme {
    private final static Float DEFAULT_FLOAT = 0F;

    public Map<String, Portrait> portrait;
    public Option option;
    public DialogBox dialogBox;
    public List<FunctionButton> functionalButton;
    public KeyPrompt keyPrompt;
    public Map<String, List<Portrait.CustomAnimation>> customAnimation;

    public ChatBoxTheme setDefaultValue() {
        for (FunctionButton button : this.functionalButton) {
            // 设置默认按钮位置
            int i = functionalButton.indexOf(button);
            button.x = BeanUtil.getValueOrDefault(button.x, (float) ((Objects.equals(button.alignX, AbstractComponent.AlignX.LEFT.name()) ? 5 : -5) * i));
        }

        return this;
    }

    public static class Portrait extends Component {
        {
            this.renderOrder = 20;
        }

        public String type;
        public String value;
        public Integer customItemData;
        public String animation;
        public Float scale = 1f;
        public List<CustomAnimation> customAnimation;
        public Boolean loop = false;
        public List<Attachment> attachment = new ArrayList<>();

        public com.zhenshiz.chatbox.component.Portrait setPortraitTheme() {
            com.zhenshiz.chatbox.component.Portrait portrait = new com.zhenshiz.chatbox.component.Portrait(com.zhenshiz.chatbox.component.Portrait.Type.of(this.type), this.animation, this.customAnimation, this.scale, this.loop);
            switch (portrait.type) {
                case TEXTURE -> portrait.createTexture(portrait, this.value, this.attachment).build();
                case PLAYER_HEAD -> portrait.createPlayerHead(portrait, this.value).build();
                case ITEM -> portrait.createItem(portrait, this.value, this.customItemData).build();
            }
            return portrait.setDefaultOption(this.x, this.y, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.opacity, this.renderOrder, this.angle);
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CustomAnimation {
            public String texture;
            public Integer time;
            public Float x;
            public Float y;
            public Float xOffset;
            public Float yOffset;
            public Float scale;
            public Float opacity;
            public Float angle;
            public EasingUtil.Easing easing;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        public static class Attachment {
            public String value;
            public Float x = DEFAULT_FLOAT;
            public Float y = DEFAULT_FLOAT;
            public Float width = DEFAULT_FLOAT;
            public Float height = DEFAULT_FLOAT;

            public Attachment mapParameter() {
                return new Attachment(this.value, AbstractComponent.getResponsiveWidth(this.x), AbstractComponent.getResponsiveHeight(this.y), AbstractComponent.getResponsiveWidth(this.width), AbstractComponent.getResponsiveHeight(this.height));
            }
        }
    }

    public static class Option extends Component {
        {
            this.renderOrder = 10;
        }

        public String texture;
        public String selectTexture;
        public String lockTexture;
        public Float optionChatX = DEFAULT_FLOAT;
        public Float optionChatY = DEFAULT_FLOAT;
        public String textAlign = ChatOption.TextAlign.LEFT.name();

        public ChatOption setChatOptionTheme(ChatOption chatOption) {
            return chatOption.setDefaultOption(this.x, this.y, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.opacity, this.renderOrder, this.angle)
                    .setTextures(this.texture)
                    .setSelectTexture(this.selectTexture)
                    .setLockTexture(this.lockTexture)
                    .setOptionChatPosition(this.optionChatX, this.optionChatY)
                    .setTextAlign(ChatOption.TextAlign.of(this.textAlign))
                    .build();
        }
    }

    public static class DialogBox extends Component {
        {
            this.renderOrder = 0;
        }

        public String texture;
        public Float lineWidth;
        public Float nameX = DEFAULT_FLOAT;
        public Float nameY = DEFAULT_FLOAT;
        public Float textX = DEFAULT_FLOAT;
        public Float textY = DEFAULT_FLOAT;

        public com.zhenshiz.chatbox.component.DialogBox setDialogBoxTheme(com.zhenshiz.chatbox.component.DialogBox dialogBox) {
            return dialogBox.setDefaultOption(this.x, this.y, this.width, this.height, AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY), this.opacity, this.renderOrder, this.angle)
                    .setTexture(this.texture)
                    .setNamePosition(this.nameX, this.nameY)
                    .setTextPosition(this.textX, this.textY)
                    .setLineWidth(this.lineWidth)
                    .build();
        }
    }

    public static class FunctionButton extends Component {
        {
            this.x = null; // 必须重置x的位置
            this.width = 5f;
            this.height = 8f;
            this.alignX = AbstractComponent.AlignX.RIGHT.name();
            this.alignY = AbstractComponent.AlignY.BOTTOM.name();
            this.renderOrder = 30;
        }

        public String type;
        public String texture;
        public String hoverTexture;

        public static List<FunctionalButton> setFunctionalButtonTheme(List<FunctionButton> functionButtons) {
            return functionButtons.stream().map(b ->
                            new FunctionalButton(FunctionalButton.Type.of(b.type))
                                    .setDefaultOption(b.x, b.y, b.width, b.height, AbstractComponent.AlignX.of(b.alignX), AbstractComponent.AlignY.of(b.alignY), b.opacity, b.renderOrder, b.angle)
                                    .setTexture(b.texture).setHoverTexture(b.hoverTexture))
                    .toList();
        }
    }

    public static class KeyPrompt extends Component {
        {
            this.renderOrder = 40;
        }

        public Boolean visible = true;
        public Float mouseTextureWidth = 16f;
        public Float mouseTextureHeight = 16f;
        public String rightClickTexture;
        public String scrollTexture;

        public KeyPromptRender setKeyPromptTheme(KeyPromptRender keyPromptRender) {
            return keyPromptRender.setPosition(this.x, this.y)
                    .setAlign(AbstractComponent.AlignX.of(this.alignX), AbstractComponent.AlignY.of(this.alignY))
                    .setOpacity(this.opacity)
                    .setRenderOrder(this.renderOrder)
                    .setVisible(this.visible)
                    .setMouseTextureSize(this.mouseTextureWidth, this.mouseTextureHeight)
                    .setRightClickTexture(this.rightClickTexture)
                    .setScrollTexture(this.scrollTexture)
                    .build();
        }
    }

    public static class Component {
        public Float x = DEFAULT_FLOAT;
        public Float y = DEFAULT_FLOAT;
        public Float width = 10f;
        public Float height = 10f;
        public String alignX = AbstractComponent.AlignX.LEFT.name();
        public String alignY = AbstractComponent.AlignY.TOP.name();
        public Float opacity = 100f;
        public Integer renderOrder;
        public Float angle = DEFAULT_FLOAT;
    }
}
