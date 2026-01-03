package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.utils.math.EasingUtil;

public class Keyframe {
    public Integer time = 1; //给时间设置默认值防止崩溃
    public Float x;
    public Float y;
    public Float xOffset;
    public Float yOffset;
    public Float scale;
    public Float brightness;
    public Float opacity;
    public Float angle;
    public EasingUtil.Easing easing;
    public String texture;
    public Attachment[] attachments;

    public Keyframe x(float x) {
        this.x = x;
        return this;
    }

    public Keyframe y(float y) {
        this.y = y;
        return this;
    }

    public Keyframe scale(float scale) {
        this.scale = scale;
        return this;
    }

    public Keyframe brightness(float brightness) {
        this.brightness = brightness;
        return this;
    }

    public Keyframe opacity(float opacity) {
        this.opacity = opacity;
        return this;
    }

    public Keyframe angle(float angle) {
        this.angle = angle;
        return this;
    }

    public Keyframe texture(String texture) {
        this.texture = texture;
        return this;
    }

    public Keyframe attachments(Attachment[] attachments) {
        this.attachments = attachments;
        return this;
    }

    public static Keyframe start(Keyframe frame, float x, float y, float scale, float brightness, float opacity, float angle) {
        return frame.x(x).y(y).scale(scale).brightness(brightness).opacity(opacity).angle(angle);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Keyframe original(Keyframe frame, float x, float y, float scale, float brightness, float opacity, float angle, String texture, Attachment[] attachments) {
        return start(frame, x, y, scale, brightness, opacity, angle).texture(texture).attachments(attachments);
    }
}
