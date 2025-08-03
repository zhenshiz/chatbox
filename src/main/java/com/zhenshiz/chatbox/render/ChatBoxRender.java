package com.zhenshiz.chatbox.render;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.Config;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.ChatOption;
import com.zhenshiz.chatbox.event.neoforge.ChatBoxRenderEvent;
import com.zhenshiz.chatbox.mixin.SoundEngineAccessor;
import com.zhenshiz.chatbox.mixin.SoundInstanceAccessor;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

@EventBusSubscriber(modid = ChatBox.MOD_ID)
public class ChatBoxRender {
    //是否打开了对话框
    public static Boolean isOpenChatBox = false;
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

            NeoForge.EVENT_BUS.post(new ChatBoxRenderEvent.Post(guiGraphics));
        }
    }

    @SubscribeEvent
    public static void ChatBoxRenderTick(ClientTickEvent.Post event) {
        if (isRenderChatBox()) {
            if (chatBoxScreen.dialogBox != null) {
                chatBoxScreen.dialogBox.tick();

                if (chatBoxScreen.autoPlay) {
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
                    if (!chatBoxScreen.dialogBox.isAllOver || chatBoxScreen.video != null && chatBoxScreen.video.isPlaying()) {
                        return;
                    }
                    chatBoxScreen.tickAutoPlay--;
                    if (chatBoxScreen.tickAutoPlay <= 0) {
                        chatBoxScreen.tickAutoPlay = 20;
                        chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void ChatBoxRenderKeyInput(InputEvent.Key event) {
        if (isRenderChatBox() && event.getAction() == 1) {
            int key = event.getKey();
            if (key == GLFW.GLFW_KEY_LEFT_CONTROL) {
                //ctrl快进
                chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
            } else if (key == GLFW.GLFW_KEY_F6) {
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
                    }

                    chatBoxScreen.dialogBox.click(chatBoxScreen.shouldGotoNext());
                }
            }
        }
    }

    @SubscribeEvent
    public static void ChatBoxRenderKeyInput(InputEvent.MouseScrollingEvent event) {
        if (isRenderChatBox()) {
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

    private static boolean isRenderChatBox() {
        return !Config.isScreen.get() && isOpenChatBox && minecraft.screen == null && chatBoxScreen.dialogBox != null;
    }
}
