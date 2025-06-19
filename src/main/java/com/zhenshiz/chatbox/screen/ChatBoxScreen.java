package com.zhenshiz.chatbox.screen;

import com.zhenshiz.chatbox.component.*;
import com.zhenshiz.chatbox.event.fabric.ChatBoxRender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChatBoxScreen extends Screen {
    public List<ChatOption> chatOptions = new ArrayList<>();
    public List<Portrait> portraits = new ArrayList<>();
    public DialogBox dialogBox = new DialogBox();
    public LogButton logButton = new LogButton();

    public ChatBoxScreen() {
        super(Component.nullToEmpty("ChatBoxScreen"));
    }

    public ChatBoxScreen addChatOptions(ChatOption chatOption) {
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
            if (ChatBoxRender.PRE.invoker().pre(guiGraphics)) return;
/*            if (NeoForge.EVENT_BUS.post(new ChatBoxRender.Pre(guiGraphics)).isCanceled()) {
                return;
            }*/

            List<AbstractComponent<?>> list = new ArrayList<>();
            list.add(dialogBox);
            if (chatOptions != null) list.addAll(chatOptions);
            if (portraits != null) list.addAll(portraits);
            if (logButton != null) list.add(logButton);

            list.sort(Comparator.comparingInt(p -> p.renderOrder));

            list.forEach(abstractComponent -> abstractComponent.render(guiGraphics, pMouseX, pMouseY));

            ChatBoxRender.POST.invoker().post(guiGraphics);
            //NeoForge.EVENT_BUS.post(new ChatBoxRender.Post(guiGraphics));
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

                dialogBox.click(!chatOptions.isEmpty());
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
