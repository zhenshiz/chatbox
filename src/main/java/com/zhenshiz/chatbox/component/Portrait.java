package com.zhenshiz.chatbox.component;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.math.EasingUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec2;

public class Portrait extends AbstractComponent<Portrait> {
    public Type type;
    public String value;
    public Integer opacity;
    public AnimationType animationType;
    public EasingUtil.Easing easing;
    public Integer scale;
    public Integer customItemData;

    private Integer targetOpacity;
    private Integer targetY;

    //是否正在执行动画
    private boolean isAnimation = false;
    //当前执行动画的时间
    private int currentAnimationTick = 0;
    //执行动画的总时长
    private int durationAnimationTick = 20;

    public Portrait(Type type){
        setType(type).build();
        defaultOption();
    }

    //texture
    public Portrait createTexture(Portrait portrait,String value,Integer opacity,String animationType,String easing,Integer duration) {
        return portrait.setValue(value)
                .setOpacity(opacity)
                .setAnimationType(animationType)
                .setEasing(easing)
                .setDurationAnimationTick(duration)
                .setIsAnimation(true);
    }

    //player_head
    public Portrait createPlayerHead(Portrait portrait,String value){
        return portrait.setValue(value);
    }

    //item
    public Portrait createItem(Portrait portrait,String value,Integer customItemData,Integer scale){
        return portrait.setValue(value)
                .setCustomItemData(customItemData)
                .setScale(scale);
    }

    public Portrait setScale(Integer scale) {
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

    public Portrait setOpacity(Integer opacity) {
        if (opacity != null && checkSize(opacity)) this.opacity = opacity;
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

    public void resetCurrentAnimationTick() {
        this.currentAnimationTick = 0;
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
        this.targetOpacity = this.opacity;
        this.targetY = this.y;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (type != null && value != null) {
            Vec2 position = getCurrentPosition();
            int x = (int) position.x;
            int y = (int) position.y;
            switch (type) {
                case TEXTURE -> {
                    if (this.isAnimation) {
                        this.currentAnimationTick++;
                        switch (this.animationType) {
                            case FADE_IN ->
                                    setOpacity((int) EasingUtil.easingFunction(0, this.targetOpacity, this.currentAnimationTick, this.durationAnimationTick, this.easing));
                            case SLIDE_IN_FROM_BOTTOM ->
                                    setPosition(this.x, (int) EasingUtil.easingFunction(this.targetY, 0, this.currentAnimationTick, this.durationAnimationTick, this.easing));
                            case BOUNCE -> {
                                int offset = 5;
                                if (this.currentAnimationTick >= this.durationAnimationTick / 2) {
                                    setPosition(this.x, (int) EasingUtil.easingFunction(this.targetY, this.targetY + offset, this.currentAnimationTick, this.durationAnimationTick, this.easing));
                                } else {
                                    setPosition(this.x, (int) EasingUtil.easingFunction(this.targetY + offset, this.targetY, this.currentAnimationTick, this.durationAnimationTick, this.easing));
                                }
                            }
                        }
                        if (this.currentAnimationTick == this.durationAnimationTick) {
                            setIsAnimation(false);
                            resetCurrentAnimationTick();
                        }
                    }
                    RenderSystem.enableBlend();
                    guiGraphics.setColor(1f, 1f, 1f, (float) this.opacity / 100);
                    renderImage(guiGraphics, ResourceLocation.parse(this.value));
                    RenderSystem.disableBlend();
                }
                case PLAYER_HEAD ->
                        RenderUtil.renderPlayerHead(guiGraphics, parseText(this.value), getResponsiveWidth(x), getResponsiveHeight(y), getResponsiveWidth(this.width) + getResponsiveHeight(this.height));
                case ITEM -> {
                    ItemStack itemStack = BuiltInRegistries.ITEM.get(ResourceLocation.parse(this.value)).getDefaultInstance();
                    if (this.customItemData != null) {
                        itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(this.customItemData));
                    }
                    RenderUtil.renderItem(guiGraphics, itemStack, getResponsiveWidth(x), getResponsiveHeight(y), this.scale);
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

    public enum AnimationType {
        NONE, // 无动画效果
        FADE_IN, // 渐入效果
        SLIDE_IN_FROM_BOTTOM, // 从底部滑入效果
        BOUNCE; // 弹跳效果

        public static AnimationType of(String type) {
            return valueOf(type.toUpperCase());
        }
    }
}
