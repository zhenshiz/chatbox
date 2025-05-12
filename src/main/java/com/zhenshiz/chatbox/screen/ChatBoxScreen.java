package com.zhenshiz.chatbox.screen;

import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.component.DialogBox;
import com.zhenshiz.chatbox.component.LogButton;
import com.zhenshiz.chatbox.component.Portrait;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChatBoxScreen extends Screen {
    public List<ChatOption> chatOptions = new ArrayList<>();
    public List<Portrait> portraits = new ArrayList<>();
    public DialogBox dialogBox = new DialogBox();
    public LogButton logButton = new LogButton();

    public ChatBoxScreen() {
        super(Component.nullToEmpty("ChatBoxScreen"));
    }

    public ChatBoxScreen setChatOptions(ChatOption chatOption) {
        if (chatOption != null) this.chatOptions.add(chatOption);
        return this;
    }

    public ChatBoxScreen setChatOptions(List<ChatOption> chatOptions) {
        if (chatOptions != null) this.chatOptions = chatOptions;
        return this;
    }

    public ChatBoxScreen setDialogBox(DialogBox dialogBox) {
        if (dialogBox != null) this.dialogBox = dialogBox;
        return this;
    }

    public ChatBoxScreen setPortrait(List<Portrait> portraits) {
        if (portraits != null) this.portraits = portraits;
        return this;
    }

    public ChatBoxScreen setLogButton(LogButton logButton) {
        if (logButton != null) this.logButton = logButton;
        return this;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (dialogBox != null) {
            dialogBox.render(guiGraphics, pMouseX, pMouseY);

            if (chatOptions != null) {
                for (ChatOption chatOption : chatOptions) {
                    chatOption.render(guiGraphics, pMouseX, pMouseY);
                }
            }

            if (portraits != null) {
                for (Portrait portrait : portraits) {
                    portrait.render(guiGraphics, pMouseX, pMouseY);
                }
            }

            if (logButton != null) {
                logButton.render(guiGraphics, pMouseX, pMouseY);
            }
        }
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) {
            if (dialogBox != null) {
                for (ChatOption chatOption : chatOptions) {
                    if (chatOption.isSelect((int) pMouseX, (int) pMouseY) && dialogBox.isAllOver) {
                        chatOption.click();
                        return super.mouseClicked(pMouseX, pMouseY, pButton);
                    }
                }

                if (logButton.isSelect((int) pMouseX, (int) pMouseY)) {
                    logButton.click();
                    return super.mouseClicked(pMouseX, pMouseY, pButton);
                }

                dialogBox.click((int) pMouseX, (int) pMouseY, !chatOptions.isEmpty());
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public void tick() {
        if (dialogBox != null) {
            dialogBox.tick();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}
