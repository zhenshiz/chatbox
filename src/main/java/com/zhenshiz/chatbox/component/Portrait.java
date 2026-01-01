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

import java.util.List;

import static com.zhenshiz.chatbox.utils.math.EasingUtil.easingFunction;

@SuppressWarnings({"unchecked", "UnusedReturnValue"})
public class Portrait<T extends Portrait<T>> extends AbstractComponent<T> {
    public Type type = Type.TEXTURE;
    public int itemCount = 1;
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
                .setItemCount(p.itemCount).setAnimationType(p.animation).setKeyframes(p.customAnimation).setLoop(p.loop).setAttachments(p.attachment);
    }

    public T setType(String type) {
        if (notNull(type)) this.type = Type.of(type);
        return (T) this;
    }

    public T setItemCount(Integer itemCount) {
        if (notNull(itemCount)) this.itemCount = itemCount;
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
            if (isAnimation) setStart(x, y, scale, brightness, opacity, angle);
        }
        return (T) this;
    }

    public T setLoop(Boolean loop) {
        if (notNull(loop) && this.isAnimation) {
            this.loop = loop;
            if (loop) setOriginal(x, y, scale, brightness, opacity, angle,
                    notNull(getTexture()) ? getTexture().toString() : null, attachments);
        }
        return (T) this;
    }

    public void updateAnimationTick() {if (this.isAnimation) this.currentFrame++;}

    public void resetCurrentFrame() {this.currentFrame = 0;}

    public T setAttachments(Attachment[] attachments) {
        if (CollUtil.notEmpty(attachments)) this.attachments = attachments;
        return (T) this;
    }

    public Attachment[] addTempAttachment(Attachment... attachments) {
        return ArrayUtils.addAll(this.attachments, attachments);
    }

    protected void setStart(float x, float y, float scale, float brightness, float opacity, float angle) {
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
                RenderUtil.renderOpacity(guiGraphics, this.brightness, this.opacity, () -> RenderUtil.renderItem(guiGraphics, stack, (int) realX(), (int) realY(), this.scale, this.angle, this.attachments));
            }
        }
    }

    //执行自定义动画
    protected void execCustomAnimation() {
        if (!this.isAnimation) return;
        var animation = this.keyframes.get(this.frameIndex);
        setTexture(animation.texture).setAttachments(animation.attachments);
        int time = this.currentFrame;
        var start = this.startKeyframe;
        // 先计算绝对坐标移动产生的偏移量，再加上相对坐标移动产生的偏移量，因此两种移动方式可以同时生效
        float curX = start.x;
        float curY = start.y;
        if (animation.x != null) curX = easingFunction(start.x, animation.x, time, animation.time, animation.easing);
        if (animation.y != null) curY = easingFunction(start.y, animation.y, time, animation.time, animation.easing);
        if (animation.xOffset != null) curX += easingFunction(0, animation.xOffset, time, animation.time, animation.easing);
        if (animation.yOffset != null) curY += easingFunction(0, animation.yOffset, time, animation.time, animation.easing);
        if (curX != start.x || curY != start.y) setPosition(curX, curY);

        if (animation.scale != null) setScale(easingFunction(start.scale, animation.scale, time, animation.time, animation.easing));
        if (animation.brightness != null) setBrightness(easingFunction(start.brightness, animation.brightness, time, animation.time, animation.easing));
        if (animation.opacity != null) setOpacity(easingFunction(start.opacity, animation.opacity, time, animation.time, animation.easing));
        if (animation.angle != null) setAngle(easingFunction(start.angle, animation.angle, time, animation.time, animation.easing));

        if (time >= animation.time) {
            setStart(this.x, this.y, this.scale, this.brightness, this.opacity, this.angle);
            this.frameIndex++;
            resetCurrentFrame();
            if (this.frameIndex >= this.keyframes.size()) {
                if (this.loop) {
                    this.frameIndex = 0;
                    var reset = this.original;
                    setStart(reset.x, reset.y, reset.scale, reset.brightness, reset.opacity, reset.angle);
                    // 由于我修改了执行动画的逻辑，现在不需要给动画设置初始值了，但是在循环播放时需要重设立绘的初始参数
                    setPosition(reset.x, reset.y).setScale(reset.scale)
                            .setBrightness(reset.brightness).setOpacity(reset.opacity).setAngle(reset.angle)
                            .setTexture(reset.texture).setAttachments(reset.attachments);
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
            try {
                return valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return TEXTURE;
            }
        }
    }
}
