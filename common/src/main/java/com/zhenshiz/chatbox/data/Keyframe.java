package com.zhenshiz.chatbox.data;

import com.zhenshiz.chatbox.component.Portrait;
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
    public Attachment[] attachment;

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
        this.attachment = attachments;
        return this;
    }

    public static Keyframe start(Keyframe frame, float x, float y, float scale, float brightness, float opacity, float angle) {
        return frame.x(x).y(y).scale(scale).brightness(brightness).opacity(opacity).angle(angle);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Keyframe original(Keyframe frame, float x, float y, float scale, float brightness, float opacity, float angle, String texture, Attachment[] attachments) {
        return start(frame, x, y, scale, brightness, opacity, angle).texture(texture).attachments(attachments);
    }

    public void apply(Portrait<?> portrait, Keyframe start, int time) {
        float progress = EasingUtil.easingFunction(0, 1, time, this.time, easing);
        Float x = applyProgress(start.x, this.x, progress);
        Float xOffset = applyProgress(0, this.xOffset, progress);
        x = x == null ? xOffset == null ? null : start.x + xOffset : Float.valueOf(xOffset == null ? x : x + xOffset);
        Float y = applyProgress(start.y, this.y, progress);
        Float yOffset = applyProgress(0, this.yOffset, progress);
        y = y == null ? yOffset == null ? null : start.y + yOffset : Float.valueOf(yOffset == null ? y : y + yOffset);

        Float scale = applyProgress(start.scale, this.scale, progress);
        Float brightness = applyProgress(start.brightness, this.brightness, progress);
        Float opacity = applyProgress(start.opacity, this.opacity, progress);
        Float angle = applyProgress(start.angle, this.angle, progress);
        portrait.setPosition(x, y).setScale(scale).setBrightness(brightness).setOpacity(opacity).setAngle(angle);
        if (time >= this.time) {
            portrait.setTexture(texture).setAttachments(attachment)
                    .setStart(portrait.x, portrait.y, portrait.scale, portrait.brightness, portrait.opacity, portrait.angle);
            portrait.nextKeyframe();
        }
    }

    private static Float applyProgress(float start, Float end, float progress) {
        if (end == null || Float.compare(end, start) == 0) return null;
        return start + (end - start) * progress;
    }
}
