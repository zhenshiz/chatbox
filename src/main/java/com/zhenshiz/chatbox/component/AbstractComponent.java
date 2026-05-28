package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.data.Attachment;
import com.zhenshiz.chatbox.component.data.CompEvtWrapper;
import com.zhenshiz.chatbox.component.data.ComponentEvent;
import com.zhenshiz.chatbox.component.data.IPosition;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "UnusedReturnValue"})
public abstract class AbstractComponent<T extends AbstractComponent<T>> implements IPosition {
    protected static final Minecraft minecraft = Minecraft.getInstance();
    //水平对齐: LEFT CENTER RIGHT
    public AlignX alignX = AlignX.LEFT;
    //垂直对齐 TOP CENTER BOTTOM
    public AlignY alignY = AlignY.TOP;
    //水平偏移 百分比
    public float x = 0;
    //垂直偏移 百分比
    public float y = 0;
    @Getter //宽度 百分比 >=100
    public float width = 10;
    @Getter public Reference widthReference = Reference.SCREEN_WIDTH;
    @Getter //高度 百分比 >=100
    public float height = 10;
    @Getter public Reference heightReference = Reference.SCREEN_HEIGHT;
    @Getter //缩放比例
    public float scale = 1;
    //亮度 百分比 >=0
    public float brightness = 100;
    //透明度 百分比 >=0
    public float opacity = 100;
    //渲染顺序
    public int renderOrder;
    //旋转角度
    public float angle = 0;
    //组件的纹理集
    public static final String ROOT = "root";
    public static final String HOVER = "hover";
    public static final String LOCK = "lock";
    public Map<String, String> textures = new HashMap<>();
    @Getter // 组件id，即主题文件中定义的组件标识，用于移除组件等操作
    public String id = "";

    // 是否隐藏，被隐藏的组件不会被渲染，不会触发事件；选项被指令隐藏的逻辑不由这个值控制，右键隐藏部分组件也不由这个值控制
    public boolean hidden = false;
    //是否上锁
    public boolean isLock = false;
    // 是否被鼠标选中，用于触发被选中时的事件
    public boolean isSelect = false;
    // 是否渲染已开始，用于触发渲染开始时的事件
    protected boolean renderStarted = false;
    @Nullable public CompEvtWrapper events;

    public T setHidden(Boolean hidden) {
        if (notNull(hidden)) this.hidden = hidden;
        return (T) this;
    }

    public T setIsLock(Boolean isLock) {
        if (notNull(isLock)) this.isLock = isLock;
        return (T) this;
    }

    /**取消锁定和隐藏状态，选项做了特殊处理*/
    public T setNormal() {
        if (this instanceof ChatOption option) option.hideOption(false); else setHidden(false);
        return setIsLock(false);
    }

    public T setIsSelect(boolean isSelect) {
        this.isSelect = isSelect;
        return (T) this;
    }

    public CompEvtWrapper getOrCreateEvents() {
        if (events == null) events = new CompEvtWrapper();
        return events;
    }

    public T setEvents(List<ComponentEvent> events) {
        if (!events.isEmpty()) getOrCreateEvents().set(events, this);
        return (T) this;
    }

    public int fireEvent(String trigger) {
        //如果组件被隐藏或者上锁，并且事件是点击事件，则不触发事件
        if (events == null || (hidden || isLock) && trigger.toUpperCase().contains("CLICK")) return 0;
        return events.fireAll(trigger);
    }

    public T addEvent(String trigger, String type, String target) {
        getOrCreateEvents().add(trigger, type, target, this);
        return (T) this;
    }

    public T of(ChatBoxTheme.Component c) {
        return setPosition(c.x, c.y).setSize(c.width, c.height).setScale(c.scale).setAlign(c.alignX, c.alignY)
                .setReference(c.widthReference, c.heightReference)
                .setBrightness(c.brightness).setOpacity(c.opacity).setRenderOrder(c.renderOrder).setAngle(c.angle).setHidden(c.hidden).setEvents(c.getEvents());
    }

    public T setPosition(Float x, Float y) {
        if (notNull(x)) this.x = x;
        if (notNull(y)) this.y = y;
        return (T) this;
    }

    public T setSize(Float width, Float height) {
        if (checkSize(width)) this.width = width;
        if (checkSize(height)) this.height = height;
        return (T) this;
    }

    public T setReference(String width, String height) {
        if (notNull(width)) this.widthReference = Reference.of(width);
        if (notNull(height)) this.heightReference = Reference.of(height);
        return (T) this;
    }

    public T setAlign(String alignX, String alignY) {
        if (notNull(alignX)) this.alignX = AlignX.of(alignX);
        if (notNull(alignY)) this.alignY = AlignY.of(alignY);
        return (T) this;
    }

    @Override
    public float realX() {
        float w = IPosition.calWidth(x);
        return switch (alignX) {
            case LEFT -> w;
            case CENTER -> w + (RenderUtil.screenWidth() - realWidth()) / 2;
            case RIGHT -> w + RenderUtil.screenWidth() - realWidth();
        };
    }
    @Override
    public float realY() {
        float h = IPosition.calHeight(y);
        return switch (alignY) {
            case TOP -> h;
            case CENTER -> h + (RenderUtil.screenHeight() - realHeight()) / 2;
            case BOTTOM -> h + RenderUtil.screenHeight() - realHeight();
        };
    }

    public T setBrightness(Float brightness) {
        if (checkSize(brightness)) this.brightness = brightness;
        return (T) this;
    }

    public T setOpacity(Float opacity) {
        if (checkSize(opacity)) this.opacity = opacity;
        return (T) this;
    }

    public T setRenderOrder(Integer renderOrder) {
        if (notNull(renderOrder)) this.renderOrder = renderOrder;
        return (T) this;
    }

    public T setScale(Float scale) {
        if (checkSize(scale)) this.scale = scale;
        return (T) this;
    }

    public T setAngle(Float angle) {
        if (notNull(angle)) this.angle = angle;
        return (T) this;
    }

    public T setTexture(String name, String texture) {
        if (notNull(name) && notNull(texture) && !texture.isEmpty()) this.textures.put(name, texture);
        return (T) this;
    }
    public @Nullable String getTexture(String name) {return textures.get(name);}

    public T setTexture(String texture) {return setTexture(ROOT, texture);}
    public @Nullable String getTexture() {return getTexture(ROOT);}

    public T setHoverTexture(String texture) {return setTexture(HOVER, texture);}
    public @Nullable String getHoverTexture() {return textures.getOrDefault(HOVER, getTexture());}

    public T setLockTexture(String texture) {return setTexture(LOCK, texture);}
    public @Nullable String getLockTexture() {return textures.getOrDefault(LOCK, getTexture());}

    /**组件当前应显示的纹理，锁定的优先级最高*/
    public @Nullable String getRenderTexture() {
        return isLock ? getLockTexture() : isSelect ? getHoverTexture() : getTexture();
    }
    public @Nullable ResourceLocation getRenderResource() {
        String texture = getRenderTexture();
        return notNull(texture) ? ChatBox.parseId(texture) : null;
    }

    public T setId(String id) {
        if (notNull(id)) this.id = id;
        return (T) this;
    }

    public static boolean notNull(Object value) {return value != null;}

    protected boolean checkSize(Float value) {return notNull(value) && value >= 0;}

    protected void renderImage(GuiGraphics guiGraphics, ResourceLocation texture, Attachment... attachments) {
        RenderUtil.renderOpacity(guiGraphics, this.brightness, this.opacity, () -> RenderUtil.renderImage(guiGraphics, texture, realX(), realY(), realWidth(), realHeight(), scale, angle, attachments));
    }

    protected String parseText(String input) {
        return ChatBoxUtil.parseText(input, true);
    }

    public String[] getDebugInfo() {
        return new String[]{
                StrUtil.format("\"x\": {}, \"y\": {}", x, y),
                StrUtil.format("\"width\": {}, \"height\": {}", width, height),
                StrUtil.format("\"scale\": {}, \"angle\": {}", scale, angle),
                StrUtil.format("\"brightness\": {}, \"opacity\": {}", brightness, opacity),
                StrUtil.format("\"renderOrder\": {}, \"id\": {}", renderOrder, getId())
        };
    }

    protected void renderInner(int mouseX, int mouseY) {
        if (!renderStarted) {
            renderStarted = true;
            fireEvent("ON_START");
        }
        if (ChatBoxUtil.isScreen) {
            if (isSelect(mouseX, mouseY)) {
                if (!isSelect) {
                    setIsSelect(true);
                    fireEvent("ON_MOUSE_OVER");
                }
            } else {
                if (isSelect) {
                    setIsSelect(false);
                    fireEvent("ON_MOUSE_OUT");
                }
            }
        }
        if (this instanceof Portrait<?> portrait) portrait.execCustomAnimation();
    }

    public abstract void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pPartialTick);

    public enum AlignX {
        LEFT,
        CENTER,
        RIGHT;

        public static AlignX of(String value) {
            return valueOf(value.toUpperCase());
        }
    }

    public enum AlignY {
        TOP,
        CENTER,
        BOTTOM;

        public static AlignY of(String value) {
            return valueOf(value.toUpperCase());
        }
    }
}
