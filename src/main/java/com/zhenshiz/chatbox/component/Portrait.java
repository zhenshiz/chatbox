package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.data.Attachment;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.data.Keyframe;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.ArrayUtils;
//? >= 1.21 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
//?}

import java.util.List;

@SuppressWarnings({"unchecked", "UnusedReturnValue"})
public class Portrait<T extends Portrait<T>> extends AbstractComponent<T> {
    public Type type = Type.TEXTURE;
    public int itemCount = 1;
    public Integer customItemData;
    //是否正在执行动画
    private boolean isAnimation = false;
    //是否循环播放动画
    public boolean loop = false;
    //自定义动画的关键帧
    public List<Keyframe> keyframes = List.of();
    //自定义动画的起始关键帧
    private final Keyframe startKeyframe = new Keyframe();
    //自定义动画的原始关键帧
    private final Keyframe original = new Keyframe();
    //当前执行动画的时间
    private int currentFrame = 0;
    //执行自定义动画的序号
    private int frameIndex = 0;
    //渲染附件
    public Attachment[] attachments = new Attachment[0];

    public T ofPortrait(ChatBoxTheme.Portrait p) {
        return of(p).setType(p.type)
                .setTexture(p.value).setTexture(p.texture).setHoverTexture(p.selectTexture).setHoverTexture(p.hoverTexture)
                .setItemCount(p.itemCount).setCustomItemData(p.customItemData).setAnimationType(p.animation).setKeyframes(p.customAnimation).setLoop(p.loop).setAttachments(p.attachment);
    }

    public T setType(String type) {
        if (notNull(type)) this.type = Type.of(type);
        return (T) this;
    }

    public T setItemCount(Integer itemCount) {
        if (notNull(itemCount)) this.itemCount = itemCount;
        return (T) this;
    }

    public T setCustomItemData(Integer customItemData) {
        this.customItemData = customItemData;
        return (T) this;
    }

    public T setAnimationType(String animationType) {
        var animations = ChatBoxUtil.animationMap;
        if (notNull(animationType) && animations.containsKey(animationType)) setKeyframes(animations.get(animationType));
        return (T) this;
    }

    public T setKeyframes(List<Keyframe> keyframes) {
        if (CollUtil.notEmpty(keyframes)) {
            this.keyframes = keyframes;
            setIsAnimation(true);
        }
        return (T) this;
    }

    public T setIsAnimation(Boolean isAnimation) {
        if (notNull(isAnimation)) {
            this.isAnimation = isAnimation;
            if (isAnimation) {
                setOriginal(x, y, scale, brightness, opacity, angle,
                        notNull(getTexture()) ? getTexture().toString() : null, attachments);
                setStart(x, y, scale, brightness, opacity, angle);
            }
        }
        return (T) this;
    }

    public T setLoop(Boolean loop) {
        if (notNull(loop) && this.isAnimation) this.loop = loop;
        return (T) this;
    }

    private void resetAnimation() {
        this.frameIndex = 0;
        var reset = this.original;
        setStart(reset.x, reset.y, reset.scale, reset.brightness, reset.opacity, reset.angle);
        // 由于我修改了执行动画的逻辑，现在不需要给动画设置初始值了，但是在循环播放时需要重设立绘的初始参数
        setPosition(reset.x, reset.y).setScale(reset.scale)
                .setBrightness(reset.brightness).setOpacity(reset.opacity).setAngle(reset.angle)
                .setTexture(reset.texture).setAttachments(reset.attachment);
    }

    public T restartAnimation() {
        if (CollUtil.isEmpty(keyframes) || loop) return (T) this;
        resetAnimation();
        return setIsAnimation(true);
    }

    public void stopAnimation() {
        if (!isAnimation || loop) return;
        for (var keyframe : keyframes) {
            if (keyframes.indexOf(keyframe) < frameIndex) continue;
            keyframe.apply(this, startKeyframe, keyframe.time);
            nextKeyframe();
        }
    }

    public void updateAnimationTick() {if (this.isAnimation) this.currentFrame++;}

    public void nextKeyframe() {
        this.frameIndex++;
        this.currentFrame = 0;
    }

    public T setAttachments(Attachment[] attachments) {
        if (CollUtil.notEmpty(attachments)) this.attachments = attachments;
        return (T) this;
    }

    public Attachment[] addTempAttachment(Attachment... attachments) {
        return ArrayUtils.addAll(this.attachments, attachments);
    }

    public void setStart(float x, float y, float scale, float brightness, float opacity, float angle) {
        Keyframe.start(this.startKeyframe, x, y, scale, brightness, opacity, angle);
    }

    protected void setOriginal(float x, float y, float scale, float brightness, float opacity, float angle, String texture, Attachment[] attachments) {
        Keyframe.original(this.original, x, y, scale, brightness, opacity, angle, texture, attachments);
    }

    @Override
    public float realWidth() {
        if (type == Type.PLAYER_HEAD) return getResponsiveWidth(width) + getResponsiveHeight(height);
        if (type == Type.ITEM) return 16;
        return super.realWidth();
    }
    @Override
    public float realHeight() {
        if (type != Type.TEXTURE) return realWidth();
        return super.realHeight();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        renderInner(mouseX, mouseY);
        var texture = isSelect ? getHoverTexture() : getTexture();
        switch (type) {
            case TEXTURE -> renderImage(guiGraphics, texture, this.attachments);
            case PLAYER_HEAD -> {
                var text = (value.isEmpty() && notNull(texture)) ? texture.getPath() : value;
                RenderUtil.renderOpacity(guiGraphics, this.brightness, this.opacity, () -> RenderUtil.renderPlayerHead(guiGraphics, parseText(text), (int) realX(), (int) realY(), (int) realWidth(), this.scale, this.angle, this.attachments));
            }
            case ITEM -> {
                var stack = notNull(texture) ?
                        new ItemStack(BuiltInRegistries.ITEM.get(texture), this.itemCount) : ItemStack.EMPTY;
                //? >= 1.21
                if (customItemData != null) stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customItemData));
                //? < 1.21 {
                /*if (customItemData != null) {
                    var tag = stack.getOrCreateTag();
                    tag.putInt("CustomModelData", customItemData);
                    stack.setTag(tag);
                }*///?}
                RenderUtil.renderOpacity(guiGraphics, this.brightness, this.opacity, () -> RenderUtil.renderItem(guiGraphics, stack, (int) realX(), (int) realY(), this.scale, this.angle, this.attachments));
            }
        }
//        int x1 = (int) realX(); int y1 = (int) realY();
//        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, x1, y1, (int) (x1 + realWidth()), (int) (y1 + realHeight()), ChatBoxClient.conf.scale, ChatBoxClient.conf.yOffset, mouseX, mouseY, minecraft.player);
    }

    //执行自定义动画
    protected void execCustomAnimation() {
        if (!this.isAnimation) return;
        if (this.frameIndex < this.keyframes.size()) {
            keyframes.get(frameIndex).apply(this, startKeyframe, currentFrame);
        } else {
            resetAnimation();
            if (!this.loop) {
                setIsAnimation(false);
                // 立绘有动画且动画播放完成时触发ON_END事件
                fireEvent("ON_END");
            }
        }
    }

    public enum Type {
        TEXTURE,
        PLAYER_HEAD,
        ITEM;

        public static Type of(String type) {
            try {
                return valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return TEXTURE;
            }
        }
    }
}
