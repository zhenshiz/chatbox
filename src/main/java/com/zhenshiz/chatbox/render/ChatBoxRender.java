package com.zhenshiz.chatbox.render;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.chatBoxScreen;

@EventBusSubscriber(modid = ChatBox.MOD_ID, value = Dist.CLIENT)
public class ChatBoxRender {
    //是否打开了对话框，包括对话框渲染层和对话框界面
    public static boolean isOpenChatBox = false;
    //上次同步对话目标实体时间
    public static long lastSyncTime = 0;
    //是否渲染对话框渲染层
    public static boolean shouldRender = false;
    //当前选择的选项序号
    public static int selectIndex = 0;
    private final static Minecraft minecraft = Minecraft.getInstance();

    @SubscribeEvent
    public static void ChatBoxRenderEvent(RenderGuiEvent.Pre event) {
        if (isRenderChatBox()) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
            chatBoxScreen.renderInner(guiGraphics, 0, 0, partialTick, false);
        }
    }

    @SubscribeEvent
    public static void ChatBoxRenderTick(ClientTickEvent.Post event) {
        if (minecraft.player == null || minecraft.player.isDeadOrDying()) onClose();
        // 客户端每5 tick请求同步对话目标实体
        if (minecraft.level != null && isOpenChatBox && minecraft.level.getGameTime() - lastSyncTime >= 5) {
            SimplePayload.simplePayloadC2S(SimplePayload.REQUEST_SYNC, "");
        }
        if (isRenderChatBox()) {
            chatBoxScreen.tick();
        }
    }

    @SubscribeEvent
    public static void ChatBoxRenderKeyInput(InputEvent.Key event) {
        if (isRenderChatBox() && chatBoxScreen.keyPromptRender.visible) {
            int key = event.getKey();
            if (key == GLFW.GLFW_KEY_LEFT_CONTROL) {
                //ctrl快进
                chatBoxScreen.dialogBoxClick();
            } else if (event.getAction() == 1 && key == GLFW.GLFW_KEY_F6) {
                //自动播放
                chatBoxScreen.autoPlay = !chatBoxScreen.autoPlay;
            }
        }
    }

    @SubscribeEvent
    public static void ChatBoxRenderMouseInput(InputEvent.MouseButton.Post event) {
        if (isRenderChatBox()) {
            if (event.getAction() == 1 && event.getButton() == 1) {
                if (chatBoxScreen.getRenderOptionCount() > 0 && chatBoxScreen.dialogBox.isAllOver) {
                    for (ChatOption option : chatBoxScreen.chatOptions) {
                        if (option.renderIndex == selectIndex && option.click()) {
                            selectIndex = 0;
                            return;
                        }
                    }
                }
                if (chatBoxScreen.keyPromptRender.visible) chatBoxScreen.dialogBoxClick();
            }
        }
    }

    @SubscribeEvent
    public static void ChatBoxRenderKeyInput(InputEvent.MouseScrollingEvent event) {
        int optionCount = chatBoxScreen.getRenderOptionCount();
        if (isRenderChatBox() && optionCount > 0) {
            double scrollDeltaY = event.getScrollDeltaY();
            if (!CollUtil.isEmpty(chatBoxScreen.chatOptions)) {
                if (scrollDeltaY > 0) { //向上
                    selectIndex = (selectIndex - 1 + optionCount) % optionCount;
                } else if (scrollDeltaY < 0) { //向下
                    selectIndex = (selectIndex + 1) % optionCount;
                }
                for (var option : chatBoxScreen.chatOptions) {
                    option.setIsSelect(selectIndex == option.renderIndex);
                }
            }
            event.setCanceled(true);
        }
    }

    public static boolean isRenderChatBox() {
        return !ChatBoxUtil.isScreen && shouldRender && minecraft.screen == null && chatBoxScreen.dialogBox != null;
    }

    //关闭对话框
    public static void onClose() {
        isOpenChatBox = false;
        shouldRender = false;
        chatBoxScreen.autoPlay = false;
        chatBoxScreen.fastForward = false;
        if (chatBoxScreen.video != null) chatBoxScreen.video.close();
        ChatBoxUtil.onCloseDialogBox();
    }
}
