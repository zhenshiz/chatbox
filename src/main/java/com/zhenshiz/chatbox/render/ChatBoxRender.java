package com.zhenshiz.chatbox.render;

import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.event.fabric.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.event.fabric.InputEvent;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.chatBoxScreen;

public class ChatBoxRender implements HudRenderCallback, ClientTickEvents.EndTick, InputEvent.Key, InputEvent.MouseButton.Post, InputEvent.MouseScrollingEvent {
    //是否打开了对话框
    public static Boolean isOpenChatBox = false;
    //当前选择的选项序号
    public static int selectIndex = 0;
    private final static Minecraft minecraft = Minecraft.getInstance();

    @Override
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        float partialTick = deltaTracker.getGameTimeDeltaTicks();
        if (isRenderChatBox()) {
            if (ChatBoxRenderEvent.PRE.invoker().pre(guiGraphics)) return;

            if (chatBoxScreen.backgroundImage != null) {
                RenderUtil.renderImage(guiGraphics, chatBoxScreen.backgroundImage, 0, 0, 0, RenderUtil.screenWidth(), RenderUtil.screenHeight(), 1);
            }

            List<AbstractComponent<?>> list = new ArrayList<>();
            list.add(chatBoxScreen.dialogBox);
            if (chatBoxScreen.video != null) list.add(chatBoxScreen.video);
            if (chatBoxScreen.chatOptions != null) list.addAll(chatBoxScreen.chatOptions);
            if (chatBoxScreen.portraits != null) list.addAll(chatBoxScreen.portraits);
            if (chatBoxScreen.keyPromptRender != null) list.add(chatBoxScreen.keyPromptRender);

            list.sort(Comparator.comparingInt(p -> p.renderOrder));

            list.forEach(abstractComponent -> abstractComponent.render(guiGraphics, partialTick));

            ChatBoxRenderEvent.POST.invoker().post(guiGraphics);
        }
    }

    @Override
    public void onEndTick(Minecraft minecraft) {
        if (isRenderChatBox()) {
            chatBoxScreen.dialogBox.tick();
        }
    }

    @Override
    public void onKey(int key, int scancode, int action, int modifiers) {
        if (isRenderChatBox()) {
            //ctrl快进
            if (key == GLFW.GLFW_KEY_LEFT_CONTROL) {
                chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
            }
        }
    }

    @Override
    public void mousePost(int button, int action, int modifiers) {
        if (isRenderChatBox()) {
            if (action == 1 && button == 1) {
                if (!CollUtil.isEmpty(chatBoxScreen.chatOptions) && chatBoxScreen.dialogBox.isAllOver) {
                    ChatOption chatOption = chatBoxScreen.chatOptions.get(selectIndex);
                    chatOption.click();
                }

                chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
            }
        }
    }

    @Override
    public boolean onMouseScroll(double scrollDeltaX, double scrollDeltaY, boolean leftDown, boolean middleDown, boolean rightDown, double mouseX, double mouseY) {
        if (isRenderChatBox() && !chatBoxScreen.chatOptions.isEmpty()) {
            if (scrollDeltaY > 0) {
                //向上
                selectIndex = (selectIndex - 1 + chatBoxScreen.chatOptions.size()) % chatBoxScreen.chatOptions.size();
            } else if (scrollDeltaY < 0) {
                //向下
                selectIndex = (selectIndex + 1) % (chatBoxScreen.chatOptions.size());
            }
            for (int i = 0; i < chatBoxScreen.chatOptions.size(); i++) {
                ChatOption chatOption = chatBoxScreen.chatOptions.get(i);
                chatOption.isSelect = i == selectIndex;
            }
            return true;
        }
        return false;
    }

    private static boolean isRenderChatBox() {
        return !ChatBoxClient.conf.isScreen && isOpenChatBox && minecraft.screen == null && chatBoxScreen.dialogBox != null;
    }
}
