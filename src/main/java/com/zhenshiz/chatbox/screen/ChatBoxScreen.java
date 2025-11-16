package com.zhenshiz.chatbox.screen;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.component.*;
import com.zhenshiz.chatbox.event.fabric.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.render.KeyPromptRender;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.chatbox.SoundUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
public class ChatBoxScreen extends Screen {
    public List<ChatOption> chatOptions = new ArrayList<>();
    public List<Portrait> portraits = new ArrayList<>();
    public DialogBox dialogBox = new DialogBox();
    public List<FunctionalButton> functionalButtons = new ArrayList<>();
    public ResourceLocation backgroundImage;
    public Boolean isEsc;
    public Boolean isPause;
    public Boolean isHistoricalSkip;
    public Video video;
    //render模式对话框用
    public KeyPromptRender keyPromptRender = new KeyPromptRender();

    public boolean fastForward = false;
    public boolean autoPlay = false;
    public int tickAutoPlay = 20;
    //是否隐藏对话框，if true，则不渲染对话框、聊天选项、功能按钮，且屏蔽交互
    public boolean hideDialogBox = false;
    public String voice = "";
    // 用于限制立绘动画的参数，单位：毫秒
    private long updateDuration = 16;
    private long lastUpdateTime = 0;

    public List<ComponentEvent> events = new ArrayList<>();

    public ChatBoxScreen() {
        super(Component.nullToEmpty("ChatBoxScreen"));
    }

    public ChatBoxScreen addChatOptions(ChatOption chatOption) {
        if (chatOption != null) this.chatOptions.add(chatOption);
        return this;
    }

    public ChatBoxScreen setChatOptions(List<ChatOption> chatOptions) {
        if (chatOptions != null) {
            this.chatOptions = chatOptions;
            for (ChatOption option : chatOptions) {
                if (StrUtil.isEmpty(option.unlockCommand)) continue;
                ChatBoxCommandUtil.simplePayloadC2S(SimplePayload.REQUEST_UNLOCK, StrUtil.merge(String.valueOf(option.isLock), String.valueOf(chatOptions.indexOf(option)), option.unlockCommand));
            }
        }
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

    public ChatBoxScreen setAnimationFPS(float fps) {
        if (fps > 0) updateDuration = (long) (1000 / fps);
        return this;
    }

    public ChatBoxScreen setKeyPromptRender(KeyPromptRender keyPromptRender) {
        if (keyPromptRender != null) this.keyPromptRender = keyPromptRender;
        return this;
    }

    public ChatBoxScreen playVoice(String voice) {
        if (StrUtil.isEmpty(voice)) {
            //如果新的一句话没有音效，根据配置决定是否中断上一句话的音效
            if (ChatBoxClient.conf.soundInterruptionEnabled) SoundUtil.stopSound(this.voice);
        } else {
            SoundUtil.stopSound(this.voice);
            SoundUtil.playSound(voice);
            this.voice = voice;
        }
        return this;
    }

    public ChatBoxScreen setEvents(List<ComponentEvent> events) {
        this.events.clear();
        if (events != null) this.events.addAll(events);
        return this;
    }

    public ChatBoxScreen fireEvent(String trigger) {
        ComponentEvent.fireAll(events, ComponentEvent.Trigger.of(trigger));
        return this;
    }

    public void setComponentHidden(String values, boolean hidden, @Nullable AbstractComponent<?> component) {
        for (String value : values.split(";")) {
            if (value.isBlank()) continue;
            value = value.trim();
            String lower = value.toLowerCase();
            // 如果为@s且组件不为空，则隐藏组件自身
            if (component != null && lower.equals("@s")) {
                component.setHidden(hidden);
                continue;
            }
            if (lower.contains("@")) { // 包含@符号以及关键字即可，增加容错
                if (lower.contains("dialog")) dialogBox.setHidden(hidden);
                if (lower.contains("options")) chatOptions.forEach(option -> option.setHidden(hidden));
                if (lower.contains("portraits")) portraits.forEach(portrait -> portrait.setHidden(hidden));
                if (lower.contains("buttons")) functionalButtons.forEach(button -> button.setHidden(hidden));
                if (lower.contains("video") && video != null) video.setHidden(hidden);
                if (lower.contains("key")) keyPromptRender.setHidden(hidden);
            } else {
                for (var portrait : portraits) {
                    if (portrait.id.equals(value)) portrait.setHidden(hidden);
                }
            }
        }
    }

    /**@return 不因指令隐藏的选项数量*/
    public int getRenderOptionCount() {
        return chatOptions.stream().filter(option -> option.renderIndex >= 0).toList().size();
    }

    private List<AbstractComponent<?>> getRenderList(boolean isScreen) {
        List<AbstractComponent<?>> list = new ArrayList<>();
        if (!hideDialogBox) {
            list.add(dialogBox);
            int i = 0; // 渲染选项时设置选项在列表中的索引
            for (ChatOption option : chatOptions) {
                if (option.renderIndex < 0) continue;
                option.renderIndex = i++;
                list.add(option);
            }
        }
        list.addAll(hideDialogBox ?
                portraits.stream().filter(portrait -> portrait.renderOrder < dialogBox.renderOrder).toList() : portraits);
        if (video != null) list.add(video);
        if (isScreen) {
            if (!hideDialogBox) list.addAll(functionalButtons);
        } else list.add(keyPromptRender);

        list.sort(Comparator.comparingInt(p -> p.renderOrder));
        return list;
    }

    public void renderInner(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick, boolean isScreen) {
        long currentTime = System.currentTimeMillis();
        boolean shouldUpdatePortrait = Math.abs(currentTime - lastUpdateTime) >= updateDuration;
        if (shouldUpdatePortrait) lastUpdateTime = currentTime;

        if (ChatBoxRenderEvent.PRE.invoker().pre(guiGraphics)) return;

        if (backgroundImage != null) {
            RenderUtil.renderImage(guiGraphics, backgroundImage, 0, 0, 0, RenderUtil.screenWidth(), RenderUtil.screenHeight(), 1, 0);
        }

        getRenderList(isScreen).forEach(component -> {
            if (!component.hidden) {
                if (shouldUpdatePortrait && component instanceof Portrait portrait) portrait.updateAnimationTick();
                component.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
            }
        });

        ChatBoxRenderEvent.POST.invoker().post(guiGraphics);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderInner(guiGraphics, pMouseX, pMouseY, pPartialTick, true);
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    public boolean shouldGotoNext() {
        //如果有视频正在播放，且视频设置为不允许跳过，则不能到下一行对话。（不会有人设置循环加不能跳过吧）
        if (video != null && video.isPlaying() && !video.canSkip) return false;
        return getRenderOptionCount() == 0;
    }

    public void dialogBoxClick() {dialogBox.click(shouldGotoNext());}

    private FunctionalButton getButton(FunctionalButton.Type type) {
        return functionalButtons.stream().filter(b -> b.type == type).findFirst().orElse(null);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (hideDialogBox) {
            hideDialogBox = false;
            return true;
        }
        if (pButton == 1) { //右键点击，隐藏对话框，同时取消快速播放
            hideDialogBox = true;
            fastForward = false;
            return true;
        }
        if (pButton == 0) {
            fastForward = false;
            for (ChatOption chatOption : chatOptions) { // 选项有最高处理优先级
                if (chatOption.isSelect && dialogBox.isAllOver && chatOption.click()) return true;
            }

            for (FunctionalButton button : functionalButtons) { // 功能按钮次之
                if (button.isSelect && button.click()) return true;
            }

            for (Portrait portrait : portraits) { // 触发立绘的点击事件，重叠的立绘只有位于最上层的才能触发
                if (portrait.isSelect && portrait.fireEvent("ON_CLICK") > 0) return true;
            }

            dialogBoxClick();
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        fastForward = false;
        if (hideDialogBox) {
            hideDialogBox = false;
            return true;
        }
        //鼠标滚轮向下滚动，操作同左键点击
        if (scrollY < 0) {
            dialogBoxClick();
            return true;
        }
        //鼠标滚轮向上滚动，打开历史记录
        if (scrollY > 0) {
            FunctionalButton logButton = getButton(FunctionalButton.Type.LOG);
            if (logButton != null && logButton.click()) return true;
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
        hideDialogBox = false;
        if (video != null) video.close();
        ChatBoxUtil.onCloseDialogBox();
        super.onClose();
    }

    @Override
    public void tick() {
        if (hideDialogBox) return; //如果隐藏对话框，则不tick
        if (!shouldGotoNext()) fastForward = false;

        dialogBox.tick();
        if (fastForward) dialogBoxClick();
        if (autoPlay) {
            // MC不在暂停游戏时tick声音，那我自己tick一下
            SoundUtil.tickWhenPaused();
            if (SoundUtil.isSoundActive(voice)) return;
            if (!dialogBox.isAllOver || video != null && video.isPlaying()) {
                return;
            }
            tickAutoPlay--;
            if (tickAutoPlay <= 0) dialogBoxClick();
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
