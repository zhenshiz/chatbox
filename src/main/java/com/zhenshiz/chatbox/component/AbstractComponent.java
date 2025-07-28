package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

import java.util.Optional;

public abstract class AbstractComponent<T extends AbstractComponent<T>> {
    protected static final Minecraft minecraft = Minecraft.getInstance();
    //水平对齐: LEFT CENTER RIGHT
    public AlignX alignX;
    //垂直对齐 TOP CENTER BOTTOM
    public AlignY alignY;
    //水平偏移 百分比 -100-100
    public float x;
    //垂直偏移 百分比 -100-100
    public float y;
    //宽度 百分比 >=0
    public float width;
    //高度 百分比 >=0
    public float height;
    //透明度
    public Float opacity;
    //渲染顺序
    public Integer renderOrder;
    //文本路径
    public ResourceLocation dialoguesResourceLocation;
    //文本分组
    public String group;
    //文本序号
    public Integer index;

    public static float getResponsiveWidth(float value) {
        return minecraft.getWindow().getGuiScaledWidth() * value / 100;
    }

    public static float getResponsiveHeight(float value) {
        return minecraft.getWindow().getGuiScaledHeight() * value / 100;
    }

    protected static <T> T getValueOrDefault(T param, T defaultValue) {
        return Optional.ofNullable(param).orElse(defaultValue);
    }

    protected void defaultOption() {
        setPosition(0, 0);
        setSize(10, 10);
        setAlign(AlignX.LEFT, AlignY.TOP);
    }

    public T setDefaultOption(float x, float y, float width, float height, AlignX alignX, AlignY alignY, Float opacity, Integer renderOrder) {
        setPosition(x, y);
        setSize(width, height);
        setAlign(alignX, alignY);
        setOpacity(opacity);
        setRenderOrder(renderOrder);
        return (T) this;
    }

    public T setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return (T) this;
    }

    public T setSize(float width, float height) {
        if (checkSize(width) && checkSize(height)) {
            this.width = width;
            this.height = height;
        }
        return (T) this;
    }

    public T setAlign(AlignX alignX, AlignY alignY) {
        if (alignX != null) this.alignX = alignX;
        if (alignY != null) this.alignY = alignY;
        return (T) this;
    }

    public T setAlignX(AlignX alignX) {
        if (alignX != null) this.alignX = alignX;
        return (T) this;
    }

    public T setAlignY(AlignY alignY) {
        if (alignY != null) this.alignY = alignY;
        return (T) this;
    }

    public T setOpacity(Float opacity) {
        if (opacity != null && checkSize(opacity)) this.opacity = opacity;
        return (T) this;
    }

    public T setRenderOrder(Integer renderOrder) {
        if (renderOrder != null) this.renderOrder = renderOrder;
        return (T) this;
    }

    public T setDialoguesInfo(ResourceLocation dialoguesResourceLocation, String group, Integer index) {
        if (dialoguesResourceLocation != null && group != null && index != null) {
            this.dialoguesResourceLocation = dialoguesResourceLocation;
            this.group = group;
            this.index = index;
        }
        return (T) this;
    }

    public T setIndex(int index) {
        this.index = index;
        return (T) this;
    }

    public T build() {
        return (T) this;
    }

    protected boolean checkSize(float value) {
        return value > 0;
    }

    protected Vec2 getCurrentPosition() {
        return new Vec2(alignX.getPositionX(this), alignY.getPositionY(this));
    }

    protected void renderImage(GuiGraphics guiGraphics, ResourceLocation texture) {
        renderImage(guiGraphics, texture, 1f);
    }

    protected void renderImage(GuiGraphics guiGraphics, ResourceLocation texture, Float scale) {
        RenderUtil.renderOpacity(guiGraphics, this.opacity / 100, () -> {
            Vec2 position = getCurrentPosition();
            RenderUtil.renderImage(guiGraphics, texture, getResponsiveWidth(position.x), getResponsiveHeight(position.y), 0, getResponsiveWidth(this.width), getResponsiveHeight(this.height), scale);
        });
    }

    public boolean isSelect(float width, float height, float x, float y, int mouseX, int mouseY) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    public boolean isSelect(int mouseX, int mouseY) {
        Vec2 position = getCurrentPosition();
        return isSelect(getResponsiveWidth(this.width), getResponsiveHeight(this.height), getResponsiveWidth((int) position.x), getResponsiveHeight((int) position.y), mouseX, mouseY);
    }

    protected String parseText(String input) {
        return ChatBoxUtil.parseText(input, false);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY,float pPartialTick){}

    public void render(GuiGraphics guiGraphics,float pPartialTick){}

    public enum AlignX {
        LEFT,
        CENTER,
        RIGHT;

        public static AlignX of(String value) {
            if (value == null) return AlignX.LEFT;
            return valueOf(value.toUpperCase());
        }

        public float getPositionX(AbstractComponent<?> abstractComponent) {
            float x = abstractComponent.x;
            float width = abstractComponent.width;
            return switch (abstractComponent.alignX) {
                case LEFT -> x;
                case CENTER -> x + 50 - width / 2;
                case RIGHT -> x + 100 - width;
                case null -> x;
            };
        }
    }

    public enum AlignY {
        TOP,
        CENTER,
        BOTTOM;

        public static AlignY of(String value) {
            if (value == null) return AlignY.TOP;
            return valueOf(value.toUpperCase());
        }

        public float getPositionY(AbstractComponent<?> abstractComponent) {
            float y = abstractComponent.y;
            float height = abstractComponent.height;
            return switch (abstractComponent.alignY) {
                case TOP -> y;
                case CENTER -> y + 50 - height / 2;
                case BOTTOM -> y + 100 - height;
                case null -> y;
            };
        }
    }
}
