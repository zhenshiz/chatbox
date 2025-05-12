package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

public abstract class AbstractComponent<T extends AbstractComponent<T>> {
    //水平对齐: LEFT CENTER RIGHT
    public AlignX alignX;
    //垂直对齐 TOP CENTER BOTTOM
    public AlignY alignY;
    //水平偏移 百分比 -100-100
    public int x;
    //垂直偏移 百分比 -100-100
    public int y;
    //图层顺序
    public int z;
    //宽度 百分比 0-100
    public int width;
    //高度 百分比 0-100
    public int height;
    //文本路径
    public ResourceLocation dialoguesResourceLocation;
    //文本分组
    public String group;
    //文本序号
    public Integer index;
    protected static final Minecraft minecraft = Minecraft.getInstance();

    public void defaultOption() {
        setPosition(0, 0);
        setSize(10, 10);
        setAlign(AlignX.LEFT, AlignY.TOP);
        setZ(0);
    }

    public T setPosition(int x, int y) {
        if (isResponsiveSkew(x) && isResponsiveSkew(y)) {
            this.x = x;
            this.y = y;
        }
        return (T) this;
    }

    public T setPosition(Integer[] position) {
        if (position != null) return setPosition(position[0], position[1]);
        return (T) this;
    }

    public T setSize(int width, int height) {
        if (isResponsiveSize(width) && isResponsiveSize(height)) {
            this.width = width;
            this.height = height;
        }
        return (T) this;
    }

    public T setSize(Integer[] size) {
        if (size != null) return setSize(size[0], size[1]);
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

    public T setZ(Integer z){
        if (z!=null) this.z = z;
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

    public static int getResponsiveWidth(int value) {
        return minecraft.getWindow().getGuiScaledWidth() * value / 100;
    }

    public static int getResponsiveHeight(int value) {
        return minecraft.getWindow().getGuiScaledHeight() * value / 100;
    }

    protected boolean isResponsiveSkew(int value) {
        return value >= -100 && value <= 100;
    }

    protected boolean isResponsiveSize(int value) {
        return value >= 0 && value <= 100;
    }

    public Vec2 getCurrentPosition() {
        return new Vec2(alignX.getPositionX(this), alignY.getPositionY(this));
    }

    public void renderImage(GuiGraphics guiGraphics, ResourceLocation texture) {
        Vec2 pos = getCurrentPosition();
        int x = (int) pos.x;
        int y = (int) pos.y;
        RenderUtil.renderImage(guiGraphics, texture, getResponsiveWidth(x), getResponsiveHeight(y), this.z, getResponsiveWidth(this.width), getResponsiveHeight(this.height));
    }

    public boolean isSelect(float width, float height, float x, float y, int mouseX, int mouseY) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    public boolean isSelect(int mouseX, int mouseY) {
        Vec2 position = getCurrentPosition();
        return isSelect(getResponsiveWidth(this.width), getResponsiveHeight(this.height), getResponsiveWidth((int) position.x), getResponsiveHeight((int) position.y), mouseX, mouseY);
    }

    public String parseText(String input) {
        return ChatBoxUtil.parseText(input, false);
    }

    public abstract void render(GuiGraphics guiGraphics, int mouseX, int mouseY);

    public enum AlignX {
        LEFT,
        CENTER,
        RIGHT;

        public int getPositionX(AbstractComponent<?> abstractComponent) {
            int x = abstractComponent.x;
            int width = abstractComponent.width;
            return switch (abstractComponent.alignX) {
                case LEFT -> x;
                case CENTER -> x + 50 - width / 2;
                case RIGHT -> x + 100 - width;
                case null -> x;
            };
        }

        public static AlignX of(String value) {
            if (value == null) return AlignX.LEFT;
            return valueOf(value.toUpperCase());
        }
    }

    public enum AlignY {
        TOP,
        CENTER,
        BOTTOM;

        public int getPositionY(AbstractComponent<?> abstractComponent) {
            int y = abstractComponent.y;
            int height = abstractComponent.height;
            return switch (abstractComponent.alignY) {
                case TOP -> y;
                case CENTER -> y + 50 - height / 2;
                case BOTTOM -> y + 100 - height;
                case null -> y;
            };
        }

        public static AlignY of(String value) {
            if (value == null) return AlignY.TOP;
            return valueOf(value.toUpperCase());
        }
    }
}
