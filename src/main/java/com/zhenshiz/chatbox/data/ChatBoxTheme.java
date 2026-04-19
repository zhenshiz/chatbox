package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.component.FunctionalButton;
import com.zhenshiz.chatbox.component.data.Attachment;
import com.zhenshiz.chatbox.component.data.ComponentEvent;
import com.zhenshiz.chatbox.component.data.Keyframe;
import com.zhenshiz.chatbox.render.KeyPromptRender;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatBoxTheme {
    private final static Float DEFAULT_FLOAT = 0F;

    public Map<String, Portrait> portrait = new HashMap<>();
    public Option option = new Option();
    public DialogBox dialogBox = new DialogBox();
    public List<FunctionButton> functionalButton = new ArrayList<>();
    public KeyPrompt keyPrompt = new KeyPrompt();
    public Map<String, List<Keyframe>> customAnimation = new HashMap<>();

    public ChatBoxTheme setDefaultValue() {
        for (FunctionButton button : this.functionalButton) {
            // 设置默认按钮位置
            int i = functionalButton.indexOf(button);
            button.x = BeanUtil.getValueOrDefault(button.x, (button.alignX.equalsIgnoreCase("left") ? 5f : -5f) * i);
        }
        return this;
    }

    public static class Portrait extends Component {
        {
            this.renderOrder = 20;
        }
        public String type;
        public String value;
        public String texture; // 与value作用相同，填一个就行，如果你非要两个都写……What can I say
        public String hoverTexture;
        public String selectTexture; // 与hoverTexture作用相同，填一个就行
        public Boolean isLock;
        public String lockTexture;
        public Integer itemCount;
        public Integer customItemData;
        public String animation;
        public List<Keyframe> customAnimation;
        public Boolean loop;
        public Attachment[] attachment;
        /*=====以下是实体渲染相关参数=====*/
        public Float yOffset;
        public String stareAt;
        public Float stareAtX;
        public Float stareAtY;

        public com.zhenshiz.chatbox.component.Portrait<?> setPortraitTheme() {
            return new com.zhenshiz.chatbox.component.Portrait<>().ofPortrait(this);
        }
    }

    public static class Option extends Portrait {
        {
            this.renderOrder = 10;
        }
        public Float optionChatX = DEFAULT_FLOAT;
        public Float optionChatY = DEFAULT_FLOAT;
        public String textAlign = "left";

        public ChatOption newOption() {
            return new ChatOption().ofPortrait(this)
                    .setOptionChatPosition(this.optionChatX, this.optionChatY)
                    .setTextAlign(this.textAlign);
        }
    }

    public static class DialogBox extends Portrait {
        {
            this.renderOrder = 0;
        }
        public Float lineWidth;
        public Float nameX = DEFAULT_FLOAT;
        public Float nameY = DEFAULT_FLOAT;
        public Float textX = DEFAULT_FLOAT;
        public Float textY = DEFAULT_FLOAT;
        public String textAlign = "left";

        public com.zhenshiz.chatbox.component.DialogBox setDialogBoxTheme(com.zhenshiz.chatbox.component.DialogBox dialogBox) {
            return dialogBox.ofPortrait(this)
                    .setNamePosition(this.nameX, this.nameY)
                    .setTextPosition(this.textX, this.textY)
                    .setTextAlign(this.textAlign)
                    .setLineWidth(this.lineWidth);
        }
    }

    public static class FunctionButton extends Portrait {
        {
            this.width = 5f;
            this.height = 8f;
            this.alignX = "right";
            this.alignY = "bottom";
            this.renderOrder = 30;
        }
    }

    public static List<FunctionalButton> setButtonTheme(List<FunctionButton> functionButtons) {
        return functionButtons.stream().map(b -> new FunctionalButton(b.type).ofPortrait(b)).toList();
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
            return keyPromptRender.of(this)
                    .setVisible(this.visible)
                    .setMouseTextureSize(this.mouseTextureWidth, this.mouseTextureHeight)
                    .setRightClickTexture(this.rightClickTexture)
                    .setScrollTexture(this.scrollTexture);
        }
    }

    public static class RenderEvent {
        public String trigger = "on_start";
        public String condition = "";
        public String type = "";
        public String value = "";
    }

    public static class Component {
        public Float x;
        public Float y;
        public Float width;
        public String widthReference;
        public Float height;
        public String heightReference;
        public Float scale;
        public String alignX;
        public String alignY;
        public Integer renderOrder;
        public Float brightness;
        public Float opacity;
        public Float angle;
        public Boolean hidden;
        public List<RenderEvent> renderEvents;

        public List<ComponentEvent> getEvents() {
            if (CollUtil.isEmpty(renderEvents)) return List.of();
            return renderEvents.stream().map(ComponentEvent::of).toList();
        }
    }
}
