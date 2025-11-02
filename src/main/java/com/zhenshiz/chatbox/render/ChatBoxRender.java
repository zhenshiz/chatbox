package com.zhenshiz.chatbox.render;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.event.neoforge.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.network.SimplePayload;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
            if (NeoForge.EVENT_BUS.post(new ChatBoxRenderEvent.Pre(guiGraphics)).isCanceled()) {
                return;
            }

            if (chatBoxScreen.backgroundImage != null) {
                RenderUtil.renderImage(guiGraphics, chatBoxScreen.backgroundImage, 0, 0, 0, RenderUtil.screenWidth(), RenderUtil.screenHeight(), 1, 0);
            }

            List<AbstractComponent<?>> list = new ArrayList<>();
            list.add(chatBoxScreen.dialogBox);
            if (chatBoxScreen.video != null) list.add(chatBoxScreen.video);
            if (chatBoxScreen.chatOptions != null) {
                int i = 0; // 渲染选项时设置选项在列表中的索引
                for (ChatOption option : chatBoxScreen.chatOptions) {
                    if (option.renderIndex < 0) continue;
                    option.renderIndex = i++;
                    list.add(option);
                }
            }
            if (chatBoxScreen.portraits != null) list.addAll(chatBoxScreen.portraits);
            if (chatBoxScreen.keyPromptRender != null) list.add(chatBoxScreen.keyPromptRender);

            list.sort(Comparator.comparingInt(p -> p.renderOrder));

            list.forEach(abstractComponent -> abstractComponent.render(guiGraphics, partialTick));

            NeoForge.EVENT_BUS.post(new ChatBoxRenderEvent.Post(guiGraphics));
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
                chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
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
                if (chatBoxScreen.dialogBox != null) {
                    if (!CollUtil.isEmpty(chatBoxScreen.chatOptions) && chatBoxScreen.dialogBox.isAllOver) {
                        ChatOption chatOption = chatBoxScreen.chatOptions.get(selectIndex);
                        chatOption.click();
                        selectIndex = 0;
                    }

                    if (chatBoxScreen.keyPromptRender.visible)
                        chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
                }
            }
        }
    }

    @SubscribeEvent
    public static void ChatBoxRenderKeyInput(InputEvent.MouseScrollingEvent event) {
        if (isRenderChatBox() && !chatBoxScreen.chatOptions.isEmpty()) {
            double scrollDeltaY = event.getScrollDeltaY();
            if (!CollUtil.isEmpty(chatBoxScreen.chatOptions)) {
                if (scrollDeltaY > 0) {
                    //向上
                    selectIndex = (selectIndex - 1 + chatBoxScreen.chatOptions.size())
                            % chatBoxScreen.chatOptions.size();
                } else if (scrollDeltaY < 0) {
                    //向下
                    selectIndex = (selectIndex + 1) % (chatBoxScreen.chatOptions.size());
                }

                for (int i = 0; i < chatBoxScreen.chatOptions.size(); i++) {
                    ChatOption chatOption = chatBoxScreen.chatOptions.get(i);
                    chatOption.isSelect = i == selectIndex;
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
