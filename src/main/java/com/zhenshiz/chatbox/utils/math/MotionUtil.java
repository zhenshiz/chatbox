package com.zhenshiz.chatbox.utils.math;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.Portrait;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class MotionUtil {
    private static final Map<String, BiFunction<Float, Float, Float>> COMPILED_EQUATIONS = new HashMap<>();
    private static final Map<String, PathCache> PATH_CACHE = new HashMap<>();

    public static void apply(Portrait portrait, String equation, Point start, float delta, int step, int index) {
        Point point = getPathPoint(equation, start, delta, step, index);
        portrait.setPosition(start.x() + point.x(), start.y() + point.y());
    }

    private static Point getPathPoint(String equation, Point startPosition, float delta, int step, int index) {
        String cacheKey = equation + "-" + startPosition + "-" + delta + "-" + step;
        if (!PATH_CACHE.containsKey(cacheKey)) {
            PATH_CACHE.put(cacheKey, new PathCache(equation, startPosition, delta, step));
        }
        if (index < 0 || index >= step) return new Point(0, 0);
        return PATH_CACHE.get(cacheKey).path[index].multiply(1, getScreenRatio());
    }

    private static class PathCache {
        private final Point[] path;

        public PathCache(String equation, Point startPosition, float delta, int step) {
            BiFunction<Float, Float, Float> compiledEquation;
            if (COMPILED_EQUATIONS.containsKey(equation)) {
                compiledEquation = COMPILED_EQUATIONS.get(equation);
            } else {
                try {
                    compiledEquation = FormulaCompiler.compile(equation);
                } catch (Exception e) {
                    ChatBox.LOGGER.error("Failed to compile equation: {}", equation, e);
                    compiledEquation = (x, y) -> 0.0f;
                }
                COMPILED_EQUATIONS.put(equation, compiledEquation);
            }
            MotionSimulator simulator = new MotionSimulator(compiledEquation, startPosition, delta);
            path = new Point[step];
            for (int i = 0; i < step; i++) {
                path[i] = simulator.getNextPosition();
            }
        }
    }

    public static float getScreenRatio() {
        Minecraft minecraft = Minecraft.getInstance();
        return (float) minecraft.getWindow().getGuiScaledWidth() / (float) minecraft.getWindow().getGuiScaledHeight();
    }
}
