package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.DialogBox;
import com.zhenshiz.chatbox.component.LogButton;
import com.zhenshiz.chatbox.screen.ChatBoxScreen;

public class DefaultChatBox {

    public static ChatBoxScreen getDefaultTheme() {
        return new ChatBoxScreen()
                .setDialogBox(new DialogBox()
                        .setSize(100, 40)
                        .setAlignY(AbstractComponent.AlignY.BOTTOM)
                        .setTexture("chatbox:textures/chatbox/default_dialog_box.png")
                        .setTextPosition(20, 10)
                        .setNamePosition(20, 15)
                        .setLineWidth(70)
                ).setLogButton(new LogButton()
                        .setSize(5, 10)
                        .setAlign(AbstractComponent.AlignX.RIGHT, AbstractComponent.AlignY.BOTTOM)
                        .setLogTexture("chatbox:textures/log/default_log.png")
                        .setHoverLogTexture("chatbox:textures/hover/default_hover_log.png")
                        .setAlign(AbstractComponent.AlignX.RIGHT, AbstractComponent.AlignY.BOTTOM)
                );
    }
}
