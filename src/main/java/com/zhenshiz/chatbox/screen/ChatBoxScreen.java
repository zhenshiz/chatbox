package com.zhenshiz.chatbox.screen;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.*;
import com.zhenshiz.chatbox.event.fabric.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.mixin.client.SoundEngineAccessor;
import com.zhenshiz.chatbox.mixin.client.SoundInstanceAccessor;
import com.zhenshiz.chatbox.render.KeyPromptRender;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChatBoxScreen extends Screen {
    public List<ChatOption> chatOptions = new ArrayList<>();
    public List<Portrait> portraits = new ArrayList<>();
    public DialogBox dialogBox = new DialogBox();
    public List<FunctionalButton> functionalButtons = new ArrayList<>();
    public ResourceLocation backgroundImage;
    public Boolean isTranslatable;
    public Boolean isEsc;
    public Boolean isPause;
    public Boolean isHistoricalSkip;
    public Video video;
    //render模式对话框用
    public KeyPromptRender keyPromptRender = new KeyPromptRender();

    public boolean fastForward = false;
    public boolean autoPlay = false;
    public int tickAutoPlay = 20;

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

    public ChatBoxScreen setFunctionalButtons(List<FunctionalButton> functionalButtons) {
        if (functionalButtons != null) this.functionalButtons = functionalButtons;
        return this;
    }

    public ChatBoxScreen setBackgroundImage(ResourceLocation backgroundImage) {
        this.backgroundImage = backgroundImage;
        return this;
    }

    public ChatBoxScreen setBackgroundImage(String backgroundImage) {
        if (backgroundImage != null) {
            return setBackgroundImage(ResourceLocation.tryParse(backgroundImage));
        } else {
            this.backgroundImage = null;
            return this;
        }
    }

    public ChatBoxScreen setVideo(Video video) {
        if (!ChatBox.isWaterMediaLoaded()) return this;
        if (this.video != null) this.video.close();
        if (video != null) this.video = video;
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

    public ChatBoxScreen setKeyPromptRender(KeyPromptRender keyPromptRender) {
        if (keyPromptRender != null) this.keyPromptRender = keyPromptRender;
        return this;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (dialogBox != null) {
            if (ChatBoxRenderEvent.PRE.invoker().pre(guiGraphics)) return;
/*            if (NeoForge.EVENT_BUS.post(new ChatBoxRender.Pre(guiGraphics)).isCanceled()) {
                return;
            }*/
            if (backgroundImage != null) {
                RenderUtil.renderImage(guiGraphics, backgroundImage, 0, 0, 0, RenderUtil.screenWidth(), RenderUtil.screenHeight(), 1);
            }

            List<AbstractComponent<?>> list = new ArrayList<>();
            list.add(dialogBox);
            if (video != null) list.add(video);
            if (chatOptions != null) list.addAll(chatOptions);
            if (portraits != null) list.addAll(portraits);
            if (functionalButtons != null) list.addAll(functionalButtons);

            list.sort(Comparator.comparingInt(p -> p.renderOrder));

            list.forEach(abstractComponent -> abstractComponent.render(guiGraphics, pMouseX, pMouseY, pPartialTick));

            ChatBoxRenderEvent.POST.invoker().post(guiGraphics);
            //NeoForge.EVENT_BUS.post(new ChatBoxRender.Post(guiGraphics));
        }
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    public boolean shouldGotoNext() {
        //如果有视频正在播放，且视频设置为不允许跳过，则不能到下一行对话。（不会有人设置循环加不能跳过吧）
        if (video != null && video.isPlaying() && !video.canSkip) return false;
        return chatOptions.isEmpty();
    }

    private FunctionalButton getButton(FunctionalButton.Type type) {
        return functionalButtons.stream().filter(b -> b.type == type).findFirst().orElse(null);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) {
            if (dialogBox != null) {
                fastForward = false;
                for (ChatOption chatOption : chatOptions) {
                    if (chatOption.isSelect(pMouseX, pMouseY) && dialogBox.isAllOver) {
                        chatOption.click();
                        return super.mouseClicked(pMouseX, pMouseY, pButton);
                    }
                }

                for (FunctionalButton button : functionalButtons) {
                    if (button.isSelect(pMouseX, pMouseY)) {
                        button.click();
                        return super.mouseClicked(pMouseX, pMouseY, pButton);
                    }
                }

                dialogBox.click(shouldGotoNext());
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        fastForward = false;
        //鼠标滚轮向下滚动，操作同左键点击
        if (scrollY < 0 && dialogBox != null) {
            dialogBox.click(shouldGotoNext());
            return true;
        }
        //鼠标滚轮向上滚动，打开历史记录
        if (scrollY > 0) {
            FunctionalButton logButton = getButton(FunctionalButton.Type.LOG);
            if (logButton != null) {
                logButton.click();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (video != null && video.isPlaying()) video.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        autoPlay = false;
        fastForward = false;
        if (video != null) video.close();
        super.onClose();
    }

    @Override
    public void tick() {
        if (!shouldGotoNext()) fastForward = false;
        if (dialogBox != null) {
            dialogBox.tick();
            if (fastForward) dialogBox.click(shouldGotoNext());

            Minecraft minecraft = Minecraft.getInstance();
            if (autoPlay) {
                var soundEngine = (SoundInstanceAccessor) ((SoundEngineAccessor) minecraft.getSoundManager()).getSoundEngine();
                // MC不在暂停游戏时tick声音，那我自己tick一下
                if (minecraft.isPaused()) soundEngine.invokeTickNonPaused();
                if (ChatBoxUtil.lastSoundResourceLocation != null) {
                    var instanceToChannel = soundEngine.getInstanceToChannel();
                    for (var soundInstance : instanceToChannel.keySet()) {
                        if (soundInstance.getLocation().equals(ChatBoxUtil.lastSoundResourceLocation)) {
                            if (minecraft.getSoundManager().isActive(soundInstance)) return;
                        }
                    }
                }
                if (!dialogBox.isAllOver || video != null && video.isPlaying()) {
                    return;
                }
                tickAutoPlay--;
                if (tickAutoPlay <= 0) {
                    tickAutoPlay = 20;
                    dialogBox.click(shouldGotoNext());
                }
            }
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
