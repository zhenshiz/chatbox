package com.zhenshiz.chatbox.utils.math;

import org.jetbrains.annotations.NotNull;

/**使用record表示二维坐标点*/
public record Point(float x, float y) {
    public static final Point ZERO = new Point(0, 0);

    public Point add(Point other) {
        return new Point(x + other.x, y + other.y);
    }

    public Point subtract(Point other) {
        return new Point(x - other.x, y - other.y);
    }

    public Point multiply(float scalar) {
        return new Point(x * scalar, y * scalar);
    }

    public Point multiply(float xScalar, float yScalar) {
        return new Point(x * xScalar, y * yScalar);
    }

    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public Point normalize() {
        float mag = magnitude();
        return mag > 0 ? multiply(1.0f / mag) : this;
    }

    @Override
    public @NotNull String toString() {
        return String.format("(%.6f, %.6f)", x, y);
    }
}
