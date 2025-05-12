package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
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
                        .setTextPosition(10, 10)
                        .setNamePosition(10, 10)
                ).setLogButton(new LogButton()
                        .setSize(5, 5)
                        .setAlign(AbstractComponent.AlignX.RIGHT, AbstractComponent.AlignY.BOTTOM)
                        .setLogTexture("chatbox:textures/log/default_log.png")
                );
    }

    private static ChatOption getDefaultOption(int index) {
        return new ChatOption()
                .setPosition(0, 30 + 8 * index)
                .setSize(35, 8)
                .setAlign(AbstractComponent.AlignX.RIGHT, AbstractComponent.AlignY.TOP)
                .setTextures("chatbox:textures/options/default_no_checked_option.png")
                .setSelectTexture("chatbox:textures/options/default_checked_option.png")
                .setLockTexture("chatbox:textures/options/default_lock_checked_option.png")
                .setOptionChatPosition(-12, -2);
    }
}
