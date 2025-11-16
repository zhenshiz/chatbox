package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec2;

import java.util.List;

import static com.zhenshiz.chatbox.utils.math.EasingUtil.easingFunction;

@SuppressWarnings("UnusedReturnValue")
public class Portrait extends AbstractComponent<Portrait> {
    public Type type;
    public String value;
    public String animationType;
    public Float scale;
    public Integer customItemData;
    public List<ChatBoxTheme.Portrait.CustomAnimation> customAnimation;
    public Boolean loop;

    private final ChatBoxTheme.Portrait.CustomAnimation targetCustomAnimation = new ChatBoxTheme.Portrait.CustomAnimation();

    private final ChatBoxTheme.Portrait.CustomAnimation startCustomAnimation = new ChatBoxTheme.Portrait.CustomAnimation();
    public List<ChatBoxTheme.Portrait.Attachment> attachments;
    // 立绘id，即主题文件中定义的立绘标识，用于移除立绘
    public String id;

    //是否正在执行动画
    private boolean isAnimation = false;
    //当前执行动画的时间
    private int currentAnimationTick = 0;
    //执行自定义动画的序号
    private int customAnimationIndex = 0;

    public Portrait(Type type, String animationType, List<ChatBoxTheme.Portrait.CustomAnimation> customAnimation, Float scale, Boolean loop) {
        setType(type).setAnimationType(animationType).setCustomAnimation(customAnimation).setLoop(loop).setScale(scale);
    }

    //texture
    public Portrait createTexture(Portrait portrait, String value, List<ChatBoxTheme.Portrait.Attachment> attachments) {
        return portrait.setValue(value).setAttachment(attachments);
    }

    //player_head
    public Portrait createPlayerHead(Portrait portrait, String value) {
        return portrait.setValue(value);
    }

    //item
    public Portrait createItem(Portrait portrait, String value, Integer customItemData) {
        return portrait.setValue(value).setCustomItemData(customItemData);
    }

    public Portrait setAttachment(List<ChatBoxTheme.Portrait.Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    public Portrait setScale(Float scale) {
        if (scale != null) this.scale = scale;
        return this;
    }

    public Portrait setType(Type type) {
        if (type != null) this.type = type;
        return this;
    }

    public Portrait setValue(String value) {
        if (value != null) this.value = value;
        return this;
    }

    public Portrait setCustomItemData(Integer customItemData) {
        if (customItemData != null) this.customItemData = customItemData;
        return this;
    }

    public Portrait setAnimationType(String animationType) {
        if (animationType != null) this.animationType = animationType;
        return this;
    }

    public Portrait setIsAnimation(Boolean isAnimation) {
        if (isAnimation != null) this.isAnimation = isAnimation;
        return this;
    }

    public Portrait setCustomAnimation(List<ChatBoxTheme.Portrait.CustomAnimation> customAnimation) {
        if (!CollUtil.isEmpty(customAnimation)) this.customAnimation = customAnimation;
        return this;
    }

    public Portrait setLoop(Boolean loop) {
        if (loop != null) this.loop = loop;
        return this;
    }

    public void updateAnimationTick() {
        if (this.isAnimation) this.currentAnimationTick++;
    }

    public void resetCurrentAnimationTick() {
        this.currentAnimationTick = 0;
    }

    public void setCustomAnimationIndex(Integer customAnimationIndex) {
        if (customAnimationIndex != null) this.customAnimationIndex = customAnimationIndex;
    }

    public Portrait setId(String id) {
        if (id != null) this.id = id;
        return this;
    }

    public void setTarget(float x, float y, float scale, float opacity, float angle) {
        this.targetCustomAnimation.x = x;
        this.targetCustomAnimation.y = y;
        this.targetCustomAnimation.scale = scale;
        this.targetCustomAnimation.opacity = opacity;
        this.targetCustomAnimation.angle = angle;
    }

    public void setStart(float x, float y, float scale, float opacity, float angle) {
        this.startCustomAnimation.x = x;
        this.startCustomAnimation.y = y;
        this.startCustomAnimation.scale = scale;
        this.startCustomAnimation.opacity = opacity;
        this.startCustomAnimation.angle = angle;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        super.render(guiGraphics, mouseX, mouseY, pPartialTick);
        if (type != null && value != null) {
            Vec2 position = getCurrentPosition();
            float x = position.x;
            float y = position.y;
            if (this.isAnimation) execCustomAnimation();
            switch (type) {
                case TEXTURE ->
                        renderImage(guiGraphics, ResourceLocation.parse(this.value), this.scale, this.attachments);
                case PLAYER_HEAD ->
                        RenderUtil.renderOpacity(guiGraphics, this.opacity / 100, () -> RenderUtil.renderPlayerHead(guiGraphics, parseText(this.value), (int) getResponsiveWidth(x), (int) getResponsiveHeight(y), (int) (getResponsiveWidth(this.width) + getResponsiveHeight(this.height)), this.scale, this.angle));
                case ITEM -> {
                    ItemStack itemStack = BuiltInRegistries.ITEM.get(ResourceLocation.parse(this.value)).getDefaultInstance();
                    if (this.customItemData != null) {
                        itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(this.customItemData));
                    }
                    RenderUtil.renderOpacity(guiGraphics, this.opacity / 100, () -> RenderUtil.renderItem(guiGraphics, itemStack, (int) getResponsiveWidth(x), (int) getResponsiveHeight(y), this.scale, this.angle));
                }
            }
        }
    }

    //执行自定义动画
    private void execCustomAnimation() {
        ChatBoxTheme.Portrait.CustomAnimation animation = this.customAnimation.get(this.customAnimationIndex);
        if (this.type.equals(Type.TEXTURE) && animation.texture != null) setValue(animation.texture);
        // 先计算绝对坐标移动产生的偏移量，再加上相对坐标移动产生的偏移量，因此两种移动方式互可以同时生效
        float curX = this.targetCustomAnimation.x;
        float curY = this.targetCustomAnimation.y;
        if (animation.x != null)
            curX = easingFunction(this.targetCustomAnimation.x, animation.x, this.currentAnimationTick, animation.time, animation.easing);
        if (animation.y != null)
            curY = easingFunction(this.targetCustomAnimation.y, animation.y, this.currentAnimationTick, animation.time, animation.easing);
        if (animation.xOffset != null)
            curX += easingFunction(0, animation.xOffset, this.currentAnimationTick, animation.time, animation.easing);
        if (animation.yOffset != null)
            curY += easingFunction(0, animation.yOffset, this.currentAnimationTick, animation.time, animation.easing);
        setPosition(curX, curY);
        if (animation.scale != null)
            setScale(easingFunction(this.targetCustomAnimation.scale, animation.scale, this.currentAnimationTick, animation.time, animation.easing));
        if (animation.opacity != null)
            setOpacity(easingFunction(this.targetCustomAnimation.opacity, animation.opacity, this.currentAnimationTick, animation.time, animation.easing));
        if (animation.angle != null)
            setAngle(easingFunction(this.targetCustomAnimation.angle, animation.angle, this.currentAnimationTick, animation.time, animation.easing));
        if (this.currentAnimationTick >= animation.time) {
            setTarget(this.x, this.y, this.scale, this.opacity, this.angle);
            setCustomAnimationIndex(this.customAnimationIndex + 1);
            resetCurrentAnimationTick();
            if (this.customAnimationIndex >= this.customAnimation.size()) {
                if (this.loop) {
                    setCustomAnimationIndex(0);
                    setTarget(this.startCustomAnimation.x, this.startCustomAnimation.y, this.startCustomAnimation.scale, this.startCustomAnimation.opacity, this.startCustomAnimation.angle);
                    // 由于我修改了执行动画的逻辑，现在不需要给动画设置初始值了，但是在循环播放时需要重设立绘的初始参数
                    setPosition(this.startCustomAnimation.x, this.startCustomAnimation.y);
                    setScale(this.startCustomAnimation.scale);
                    setOpacity(this.startCustomAnimation.opacity);
                    setAngle(this.startCustomAnimation.angle);
                } else {
                    setIsAnimation(false);
                    // 立绘有动画且动画播放完成时触发ON_END事件
                    fireEvent("ON_END");
                }
            }
        }
    }


    public enum Type {
        TEXTURE,
        PLAYER_HEAD,
        ITEM;

        public static Type of(String type) {
            return valueOf(type.toUpperCase());
        }
    }
}
