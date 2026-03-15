package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.Config;
import com.zhenshiz.chatbox.component.data.IPosition;
import com.zhenshiz.chatbox.component.data.Attachment;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.component.data.Keyframe;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

@SuppressWarnings({"unchecked", "UnusedReturnValue"})
public class Portrait<T extends Portrait<T>> extends AbstractComponent<T> {
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

    protected T ofCommon(ChatBoxTheme.Portrait p) {
        return of(p).setTexture(p.value).setTexture(p.texture).setHoverTexture(p.selectTexture).setHoverTexture(p.hoverTexture)
                .setIsLock(p.isLock).setLockTexture(p.lockTexture)
                .setAttachments(p.attachment).setAnimationType(p.animation).setKeyframes(p.customAnimation).setLoop(p.loop);
    }

    public T ofPortrait(ChatBoxTheme.Portrait p) {
        String type = notNull(p.type) ? p.type.toLowerCase() : "texture";
        return switch (type) {
            case "player_head" -> (T) new PlayerHead().ofCommon(p);
            case "item" -> (T) new Item().ofCommon(p).ofItem(p);
            case "entity" -> (T) new Entity().ofCommon(p).ofEntity(p);
            default -> ofCommon(p);
        };
    }

    public T setAnimationType(String animationType) {
        var animations = ChatBoxUtil.animationMap;
        if (notNull(animationType) && animations.containsKey(animationType)) setKeyframes(animations.get(animationType));
        return (T) this;
    }

    public T setKeyframes(List<Keyframe> keyframes) {
        if (notNull(keyframes)) {
            this.keyframes = keyframes;
            setIsAnimation(true);
        }
        return (T) this;
    }

    public T setIsAnimation(Boolean isAnimation) {
        if (notNull(isAnimation)) {
            this.isAnimation = isAnimation;
            if (isAnimation) {
                setOriginal(x, y, scale, brightness, opacity, angle, getTexture(), attachments);
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
        if (keyframes.isEmpty() || loop) return (T) this;
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
        if (notNull(attachments)) this.attachments = attachments;
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
        if (getClass().equals(Portrait.class)) return IPosition.calWidth(width) * Config.portraitWidthPercent.get() / 100.0F;
        return super.realWidth();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        renderInner(mouseX, mouseY);
        renderImage(guiGraphics, getRenderResource(), this.attachments);
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

    public static class PlayerHead extends Portrait<PlayerHead> {
        @Override
        public float realWidth() {return IPosition.calWidth(width) + IPosition.calHeight(height);}
        @Override
        public float realHeight() {return realWidth();}

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
            renderInner(mouseX, mouseY);
            var text = BeanUtil.getValueOrDefault(getRenderTexture(), "@s");
            RenderUtil.renderOpacity(guiGraphics, this.brightness, this.opacity, () -> RenderUtil.renderPlayerHead(guiGraphics, parseText(text), (int) realX(), (int) realY(), (int) realWidth(), this.scale, this.angle, this.attachments));
        }
    }

    public static class Item extends Portrait<Item> {
        public int itemCount = 1;
        public Integer customItemData;

        public float realWidth() {return 16;}
        public float realHeight() {return 16;}

        public Item ofItem(ChatBoxTheme.Portrait p) {
            if (notNull(p.itemCount)) itemCount = p.itemCount;
            if (notNull(p.customItemData)) customItemData = p.customItemData;
            return this;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
            renderInner(mouseX, mouseY);
            var texture = getRenderResource();
            var stack = notNull(texture) ? new ItemStack(BuiltInRegistries.ITEM.get(texture), this.itemCount) : ItemStack.EMPTY;
            if (customItemData != null) stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customItemData));
            RenderUtil.renderOpacity(guiGraphics, this.brightness, this.opacity, () -> RenderUtil.renderItem(guiGraphics, stack, (int) realX(), (int) realY(), this.scale, this.angle, this.attachments));
        }
    }

    public static class Entity extends Portrait<Entity> {
        public float yOffset = 0;
        public String stareAt = "point";
        public Float stareAtX;
        public Float stareAtY;

        public Entity ofEntity(ChatBoxTheme.Portrait p) {
            if (notNull(p.yOffset)) yOffset = p.yOffset;
            if (notNull(p.stareAt)) stareAt = p.stareAt;
            if (notNull(p.stareAtX)) stareAtX = p.stareAtX;
            if (notNull(p.stareAtY)) stareAtY = p.stareAtY;
            return this;
        }

        public float realWidth() {return width;}
        public float realHeight() {return height;}

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick) {
            renderInner(mouseX, mouseY);
            String e = BeanUtil.getValueOrDefault(getRenderTexture(), "@s");
            if (e.isEmpty()) return;
            LivingEntity entity = null;
            if (e.equals("@s")) entity = minecraft.player;
            if (e.startsWith("target")) {
                try {
                    int index = e.length() == 6 ? 0 : Integer.parseInt(e.substring(6)) - 1;
                    entity = (LivingEntity) ChatBoxUtil.chatTargets.get(index);
                } catch (Exception exc) {
                    return; // 每帧都渲染就不输出错误信息了，防止污染日志
                }
            }
            if (entity == null) return;
            float stareX, stareY;
            if (stareAt.equalsIgnoreCase("mouse") && ChatBoxUtil.isScreen) {
                stareX = mouseX; stareY = mouseY;
            } else if (stareAt.equalsIgnoreCase("point") && notNull(stareAtX) && notNull(stareAtY)) {
                stareX = IPosition.calWidth(stareAtX); stareY = IPosition.calHeight(stareAtY);
            } else {
                stareX = (float) (x1() + x2()) / 2; stareY = (float) (y1() + y2()) / 2;
            }
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, x1(), y1(), x2(), y2(), (int) (55 * scale), yOffset, stareX, stareY, entity);
        }
    }
}
