package com.zhenshiz.chatbox.utils.math;

import lombok.Getter;

import java.util.function.BiFunction;

/**
 * 模拟运动的类，用于计算一个点在给定时间步长下的下一个位置。
 */
public class MotionSimulator {
    private final BiFunction<Float, Float, Float> equation;
    @Getter
    private Point currentPosition;
    private final float delta;
    private static final float EPSILON = 0.001f;
    private static final float PROJECTION_TOLERANCE = 1e-6f;
    private static final int MAX_PROJECTION_ITERATIONS = 10;

    public MotionSimulator(BiFunction<Float, Float, Float> equation, Point startPosition, float delta) {
        this.equation = equation;
        this.currentPosition = startPosition;
        this.delta = delta;
    }

    // 计算方程在给定点的值
    private float evaluateEquation(Point p) {
        return equation.apply(p.x(), p.y());
    }

    // 计算梯度
    private Point computeGradient(Point p) {
        float gradX = (evaluateEquation(new Point(p.x() + EPSILON, p.y())) -
                evaluateEquation(new Point(p.x() - EPSILON, p.y()))) / (2 * EPSILON);

        float gradY = (evaluateEquation(new Point(p.x(), p.y() + EPSILON)) -
                evaluateEquation(new Point(p.x(), p.y() - EPSILON))) / (2 * EPSILON);

        return new Point(gradX, gradY);
    }

    // 将点投影回曲面上
    private Point projectToSurface(Point p) {
        Point projected = p;

        for (int i = 0; i < MAX_PROJECTION_ITERATIONS; i++) {
            float f = evaluateEquation(projected);
            if (Math.abs(f) < PROJECTION_TOLERANCE) break;

            Point gradient = computeGradient(projected);
            float gradientSqr = gradient.x() * gradient.x() + gradient.y() * gradient.y();

            if (gradientSqr < 1e-12f) break; // 避免除零

            float adjustment = f / gradientSqr;
            projected = projected.subtract(gradient.multiply(adjustment));
        }

        return projected;
    }

    // 获取下一个位置
    public Point getNextPosition() {
        Point gradient = computeGradient(currentPosition);

        // 计算切线方向（垂直于梯度）
        Point tangent = new Point(-gradient.y(), gradient.x()).normalize();

        // 沿切线方向移动
        Point nextPos = currentPosition.add(tangent.multiply(delta));

        // 投影回曲面上
        nextPos = projectToSurface(nextPos);

        currentPosition = nextPos;
        return nextPos;
    }
}
