package com.zhenshiz.chatbox.render;

import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.event.fabric.InputEvent;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.chatBoxScreen;

public class ChatBoxRender implements HudRenderCallback, ClientTickEvents.EndTick, InputEvent.Key, InputEvent.MouseButton.Post, InputEvent.MouseScrollingEvent {
    //是否打开了对话框，包括对话框渲染层和对话框界面
    public static boolean isOpenChatBox = false;
    //上次同步对话目标实体时间
    public static long lastSyncTime = 0;
    //是否渲染对话框渲染层
    public static boolean shouldRender = false;
    //当前选择的选项序号
    public static int selectIndex = 0;
    private final static Minecraft minecraft = Minecraft.getInstance();

    @Override
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        float partialTick = deltaTracker.getGameTimeDeltaTicks();
        if (isRenderChatBox()) {
            chatBoxScreen.renderInner(guiGraphics, 0, 0, partialTick, false);
        }
    }

    @Override
    public void onEndTick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.player.isDeadOrDying()) onClose();
        // 客户端每5 tick请求同步对话目标实体
        if (minecraft.level != null && isOpenChatBox && minecraft.level.getGameTime() - lastSyncTime >= 5) {
            ChatBoxCommandUtil.simplePayloadC2S(SimplePayload.REQUEST_SYNC, "");
        }
        if (isRenderChatBox()) {
            chatBoxScreen.tick();
        }
    }

    @Override
    public void onKey(int key, int scancode, int action, int modifiers) {
        // System.out.println("key: " + key + " scancode: " + scancode + " action: " + action + " mod: " + modifiers);
        if (isRenderChatBox() && chatBoxScreen.keyPromptRender.visible) {
            //ctrl快进
            if (key == GLFW.GLFW_KEY_LEFT_CONTROL) {
                chatBoxScreen.dialogBoxClick();
            }
            if (action == 1 && key == GLFW.GLFW_KEY_F6) {
                //自动播放
                chatBoxScreen.autoPlay = !chatBoxScreen.autoPlay;
            }
        }
    }

    @Override
    public void mousePost(int button, int action, int modifiers) {
        if (isRenderChatBox()) {
            if (action == 1 && button == 1) {
                if (chatBoxScreen.getRenderOptionCount() > 0 && chatBoxScreen.dialogBox.isAllOver) {
                    chatBoxScreen.chatOptions.stream().filter(option -> option.renderIndex == selectIndex).findFirst().ifPresent(ChatOption::click);
                    selectIndex = 0;
                }

                if (chatBoxScreen.keyPromptRender.visible) chatBoxScreen.dialogBoxClick();
            }
        }
    }

    @Override
    public boolean onMouseScroll(double scrollDeltaX, double scrollDeltaY, boolean leftDown, boolean middleDown, boolean rightDown, double mouseX, double mouseY) {
        int optionCount = chatBoxScreen.getRenderOptionCount();
        if (isRenderChatBox() && optionCount > 0) {
            if (scrollDeltaY > 0) { //向上
                selectIndex = (selectIndex - 1 + optionCount) % optionCount;
            } else if (scrollDeltaY < 0) { //向下
                selectIndex = (selectIndex + 1) % optionCount;
            }
            for (var option : chatBoxScreen.chatOptions) {
                option.setIsSelect(selectIndex == option.renderIndex);
            }
            return true;
        }
        return false;
    }

    public static boolean isRenderChatBox() {
        return !ChatBoxUtil.isScreen && shouldRender && minecraft.screen == null && chatBoxScreen.dialogBox != null;
    }

    public static void onClose() {
        isOpenChatBox = false;
        shouldRender = false;
        chatBoxScreen.autoPlay = false;
        chatBoxScreen.fastForward = false; // 这行没必要
        if (chatBoxScreen.video != null) chatBoxScreen.video.close();
        ChatBoxUtil.onCloseDialogBox();
    }
}
