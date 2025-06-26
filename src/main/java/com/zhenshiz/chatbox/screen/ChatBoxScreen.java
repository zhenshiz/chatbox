package com.zhenshiz.chatbox.screen;

import com.zhenshiz.chatbox.component.*;
import com.zhenshiz.chatbox.event.neoforge.ChatBoxRender;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChatBoxScreen extends Screen {
    public List<ChatOption> chatOptions = new ArrayList<>();
    public List<Portrait> portraits = new ArrayList<>();
    public DialogBox dialogBox = new DialogBox();
    public LogButton logButton = new LogButton();
    public ResourceLocation backgroundImage;
    public Boolean isTranslatable;
    public Boolean isEsc;
    public Boolean isPause;
    public Boolean isHistoricalSkip;
    public Integer maxTriggerCount;

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

    public ChatBoxScreen setBackgroundImage(ResourceLocation backgroundImage) {
        if (backgroundImage != null) this.backgroundImage = backgroundImage;
        return this;
    }

    public ChatBoxScreen setBackgroundImage(String backgroundImage) {
        if (backgroundImage != null) return setBackgroundImage(ResourceLocation.tryParse(backgroundImage));
        return this;
    }

    public ChatBoxScreen setIsTranslatable(Boolean isTranslatable) {
        if (isTranslatable != null) this.isTranslatable = isTranslatable;
        return this;
    }

    public ChatBoxScreen setIsEsc(Boolean isEsc) {
        if (isEsc != null) this.isEsc = isEsc;
        return this;
    }

    public ChatBoxScreen setIsPause(Boolean isPause) {
        if (isPause != null) this.isPause = isPause;
        return this;
    }

    public ChatBoxScreen setIsHistoricalSkip(Boolean isHistoricalSkip) {
        if (isHistoricalSkip != null) this.isHistoricalSkip = isHistoricalSkip;
        return this;
    }

    public ChatBoxScreen setMaxTriggerCount(Integer maxTriggerCount) {
        if (maxTriggerCount != null) this.maxTriggerCount = maxTriggerCount;
        return this;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (dialogBox != null) {
            if (NeoForge.EVENT_BUS.post(new ChatBoxRender.Pre(guiGraphics)).isCanceled()) {
                return;
            }

            if (backgroundImage != null) {
                RenderUtil.renderImage(guiGraphics, backgroundImage, 0, 0, 0, RenderUtil.screenWidth(), RenderUtil.screenHeight(), 1);
            }

            List<AbstractComponent<?>> list = new ArrayList<>();
            list.add(dialogBox);
            if (chatOptions != null) list.addAll(chatOptions);
            if (portraits != null) list.addAll(portraits);
            if (logButton != null) list.add(logButton);

            list.sort(Comparator.comparingInt(p -> p.renderOrder));

            list.forEach(abstractComponent -> abstractComponent.render(guiGraphics, pMouseX, pMouseY));

            NeoForge.EVENT_BUS.post(new ChatBoxRender.Post(guiGraphics));
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (chatOptions.isEmpty()) {
            //鼠标滚轮向下滚动，操作同左键点击
            if (scrollY < 0 && dialogBox != null) {
                dialogBox.click(false);
                return true;
            }
        }
        //鼠标滚轮向上滚动，打开历史记录
        if (scrollY > 0) {
            logButton.click();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        if (dialogBox != null) {
            dialogBox.tick();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.isEsc;
    }

    @Override
    public boolean isPauseScreen() {
        return this.isPause;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}
