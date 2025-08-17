package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import com.zhenshiz.chatbox.utils.math.EasingUtil;
import lombok.NoArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

import java.util.List;

@NoArgsConstructor
public class Portrait extends AbstractComponent<Portrait> {
    public Type type;
    public String value;
    public AnimationType animationType;
    public EasingUtil.Easing easing;
    public Float scale;
    public Integer customItemData;
    public List<ChatBoxTheme.Portrait.CustomAnimation> customAnimation;
    public Boolean loop;

    private final ChatBoxTheme.Portrait.CustomAnimation targetCustomAnimation = new ChatBoxTheme.Portrait.CustomAnimation();

    private final ChatBoxTheme.Portrait.CustomAnimation startCustomAnimation = new ChatBoxTheme.Portrait.CustomAnimation();

    //是否正在执行动画
    private boolean isAnimation = false;
    //当前执行动画的时间
    private int currentAnimationTick = 0;
    //执行动画的总时长
    private int durationAnimationTick = 20;
    //执行自定义动画的序号
    private int customAnimationIndex = 0;

    public Portrait(Type type, List<ChatBoxTheme.Portrait.CustomAnimation> customAnimation, Float scale, Boolean loop) {
        setType(type).setCustomAnimation(customAnimation).setLoop(loop).setScale(scale).build();
        defaultOption();
    }

    //texture
    public Portrait createTexture(Portrait portrait, String value, String animationType, String easing, Integer duration) {
        return portrait.setValue(value)
                .setAnimationType(animationType)
                .setEasing(easing)
                .setDurationAnimationTick(duration);
    }

    //player_head
    public Portrait createPlayerHead(Portrait portrait, String value) {
        return portrait.setValue(value);
    }

    //item
    public Portrait createItem(Portrait portrait, String value, Integer customItemData) {
        return portrait.setValue(value)
                .setCustomItemData(customItemData);
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

    public Portrait setAnimationType(AnimationType animationType) {
        if (animationType != null) this.animationType = animationType;
        return this;
    }

    public Portrait setAnimationType(String animationType) {
        if (animationType != null) return setAnimationType(AnimationType.of(animationType));
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

    public void resetCurrentAnimationTick() {
        this.currentAnimationTick = 0;
    }

    public void setCustomAnimationIndex(Integer customAnimationIndex) {
        if (customAnimationIndex != null) this.customAnimationIndex = customAnimationIndex;
    }

    public Portrait setDurationAnimationTick(Integer durationAnimationTick) {
        if (durationAnimationTick != null) this.durationAnimationTick = durationAnimationTick;
        return this;
    }

    public Portrait setEasing(EasingUtil.Easing easing) {
        if (easing != null) this.easing = easing;
        return this;
    }

    public Portrait setEasing(String easing) {
        if (easing != null) return setEasing(EasingUtil.Easing.of(easing));
        return this;
    }

    public void setTarget() {
        this.targetCustomAnimation.y = this.y;
        this.targetCustomAnimation.opacity = this.opacity;
    }

    public void setTarget(float x, float y, float scale, float opacity) {
        this.targetCustomAnimation.x = x;
        this.targetCustomAnimation.y = y;
        this.targetCustomAnimation.scale = scale;
        this.targetCustomAnimation.opacity = opacity;
    }

    public void setStart(float x, float y, float scale, float opacity) {
        this.startCustomAnimation.x = x;
        this.startCustomAnimation.y = y;
        this.startCustomAnimation.scale = scale;
        this.startCustomAnimation.opacity = opacity;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        if (type != null && value != null) {
            Vec2 position = getCurrentPosition();
            float x = position.x;
            float y = position.y;
            if (this.isAnimation) this.currentAnimationTick++;
            switch (type) {
                case TEXTURE -> {
                    if (this.isAnimation) {
                        switch (this.animationType) {
                            case FADE_IN -> {
                                setOpacity(EasingUtil.easingFunction(0, this.targetCustomAnimation.opacity, this.currentAnimationTick, this.durationAnimationTick, this.easing));
                                if (this.currentAnimationTick == this.durationAnimationTick) setIsAnimation(false);
                            }
                            case SLIDE_IN_FROM_BOTTOM -> {
                                setPosition(this.x, EasingUtil.easingFunction(this.targetCustomAnimation.y, 0, this.currentAnimationTick, this.durationAnimationTick, this.easing));
                                if (this.currentAnimationTick == this.durationAnimationTick) setIsAnimation(false);
                            }
                            case BOUNCE -> {
                                int offset = 5;
                                if (this.currentAnimationTick >= this.durationAnimationTick / 2) {
                                    setPosition(this.x, EasingUtil.easingFunction(this.targetCustomAnimation.y, this.targetCustomAnimation.y + offset, this.currentAnimationTick, this.durationAnimationTick, this.easing));
                                } else {
                                    setPosition(this.x, EasingUtil.easingFunction(this.targetCustomAnimation.y + offset, this.targetCustomAnimation.y, this.currentAnimationTick, this.durationAnimationTick, this.easing));
                                }
                                if (this.currentAnimationTick == this.durationAnimationTick) setIsAnimation(false);
                            }
                            case CUSTOM -> execCustomAnimation();
                        }
                    }
                    renderImage(guiGraphics, new ResourceLocation(this.value), getValueOrDefault(this.scale, 1f));
                }
                case PLAYER_HEAD -> {
                    if (this.isAnimation) execCustomAnimation();
                    RenderUtil.renderOpacity(guiGraphics, this.opacity / 100, () -> RenderUtil.renderPlayerHead(guiGraphics, parseText(this.value), (int) getResponsiveWidth(x), (int) getResponsiveHeight(y), (int) (getResponsiveWidth(this.width) + getResponsiveHeight(this.height)), getValueOrDefault(this.scale, 1f)));
                }
                case ITEM -> {
                    if (this.isAnimation) execCustomAnimation();
                    ItemStack itemStack = BuiltInRegistries.ITEM.get(new ResourceLocation(this.value)).getDefaultInstance();
                    if (this.customItemData != null) {
                        CompoundTag tag = itemStack.getOrCreateTag();
                        tag.putInt("CustomModelData", this.customItemData);
                        itemStack.setTag(tag);
                    }
                    RenderUtil.renderOpacity(guiGraphics, this.opacity / 100, () -> RenderUtil.renderItem(guiGraphics, itemStack, (int) getResponsiveWidth(x), (int) getResponsiveHeight(y), this.scale));
                }
            }
        }
    }

    //执行自定义动画
    private void execCustomAnimation() {
        if (this.customAnimationIndex == this.customAnimation.size()) {
            if (this.loop) {
                setCustomAnimationIndex(0);
                setTarget(this.startCustomAnimation.x, this.startCustomAnimation.y, this.startCustomAnimation.scale, this.startCustomAnimation.opacity);
            } else {
                setIsAnimation(false);
                return;
            }
        }
        ChatBoxTheme.Portrait.CustomAnimation customAnimation = this.customAnimation.get(this.customAnimationIndex);
        if (this.type.equals(Type.TEXTURE)) setValue(customAnimation.texture);
        setPosition(EasingUtil.easingFunction(this.targetCustomAnimation.x, customAnimation.x, this.currentAnimationTick, customAnimation.time, customAnimation.easing), EasingUtil.easingFunction(this.targetCustomAnimation.y, customAnimation.y, this.currentAnimationTick, customAnimation.time, customAnimation.easing));
        setScale(EasingUtil.easingFunction(this.targetCustomAnimation.scale, customAnimation.scale, this.currentAnimationTick, customAnimation.time, customAnimation.easing));
        setOpacity(EasingUtil.easingFunction(this.targetCustomAnimation.opacity, customAnimation.opacity, this.currentAnimationTick, customAnimation.time, customAnimation.easing));
        if (this.currentAnimationTick == customAnimation.time) {
            setTarget(this.x, this.y, this.scale, this.opacity);
            setCustomAnimationIndex(this.customAnimationIndex + 1);
            resetCurrentAnimationTick();
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

    public enum AnimationType {
        NONE, // 无动画效果
        FADE_IN, // 渐入效果
        SLIDE_IN_FROM_BOTTOM, // 从底部滑入效果
        BOUNCE, // 弹跳效果
        CUSTOM; //自定义动画

        public static AnimationType of(String type) {
            return valueOf(type.toUpperCase());
        }
    }
}
