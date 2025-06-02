package com.zhenshiz.chatbox.utils.math;

public class EasingUtil {

    public static double easingFunction(float min, float max, double currentTime, double duration, Easing easing) {
        double progress = currentTime / duration;
        progress = Math.min(1.0, Math.max(0.0, progress));

        double easedProgress = switch (easing) {
            case EASE_IN_SINE -> easeInSine(progress);
            case EASE_OUT_SINE -> easeOutSine(progress);
            case EASE_IN_OUT_SINE -> easeInOutSine(progress);
            case EASE_IN_QUAD -> easeInQuad(progress);
            case EASE_OUT_QUAD -> easeOutQuad(progress);
            case EASE_IN_OUT_QUAD -> easeInOutQuad(progress);
            case EASE_IN_CUBIC -> easeInCubic(progress);
            case EASE_OUT_CUBIC -> easeOutCubic(progress);
            case EASE_IN_OUT_CUBIC -> easeInOutCubic(progress);
            case EASE_IN_QUART -> easeInQuart(progress);
            case EASE_OUT_QUART -> easeOutQuart(progress);
            case EASE_IN_OUT_QUART -> easeInOutQuart(progress);
            case EASE_IN_QUINT -> easeInQuint(progress);
            case EASE_OUT_QUINT -> easeOutQuint(progress);
            case EASE_IN_OUT_QUINT -> easeInOutQuint(progress);
            case EASE_IN_EXPO -> easeInExpo(progress);
            case EASE_OUT_EXPO -> easeOutExpo(progress);
            case EASE_IN_OUT_EXPO -> easeInOutExpo(progress);
            case EASE_IN_CIRC -> easeInCirc(progress);
            case EASE_OUT_CIRC -> easeOutCirc(progress);
            case EASE_IN_OUT_CIRC -> easeInOutCirc(progress);
            case EASE_IN_BACK -> easeInBack(progress);
            case EASE_OUT_BACK -> easeOutBack(progress);
            case EASE_IN_OUT_BACK -> easeInOutBack(progress);
            case EASE_IN_ELASTIC -> easeInElastic(progress);
            case EASE_OUT_ELASTIC -> easeOutElastic(progress);
            case EASE_IN_OUT_ELASTIC -> easeInOutElastic(progress);
            case EASE_IN_BOUNCE -> easeInBounce(progress);
            case EASE_OUT_BOUNCE -> easeOutBounce(progress);
            case EASE_IN_OUT_BOUNCE -> easeInOutBounce(progress);
            case null -> easeInSine(progress);
        };
        return min + (max - min) * easedProgress;
    }

    private static double easeInSine(double x) {
        return 1 - Math.cos(x * Math.PI / 2);
    }

    private static double easeOutSine(double x) {
        return Math.sin((x * Math.PI) / 2);
    }

    private static double easeInOutSine(double x) {
        return -(Math.cos(Math.PI * x) - 1) / 2;
    }

    private static double easeInQuad(double x) {
        return x * x;
    }

    private static double easeOutQuad(double x) {
        return 1 - (1 - x) * (1 - x);
    }

    private static double easeInOutQuad(double x) {
        return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2;
    }

    private static double easeInCubic(double x) {
        return x * x * x;
    }

    private static double easeOutCubic(double x) {
        return 1 - Math.pow(1 - x, 3);
    }

    private static double easeInOutCubic(double x) {
        return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
    }

    private static double easeInQuart(double x) {
        return x * x * x * x;
    }

    private static double easeOutQuart(double x) {
        return 1 - Math.pow(1 - x, 4);
    }

    private static double easeInOutQuart(double x) {
        return x < 0.5 ? 8 * x * x * x * x : 1 - Math.pow(-2 * x + 2, 4) / 2;
    }

    private static double easeInQuint(double x) {
        return x * x * x * x * x;
    }

    private static double easeOutQuint(double x) {
        return 1 - Math.pow(1 - x, 5);
    }

    private static double easeInOutQuint(double x) {
        return x < 0.5 ? 16 * x * x * x * x * x : 1 - Math.pow(-2 * x + 2, 5) / 2;
    }

    private static double easeInExpo(double x) {
        return x == 0 ? 0 : Math.pow(2, 10 * x - 10);
    }

    private static double easeOutExpo(double x) {
        return x == 1 ? 1 : 1 - Math.pow(2, -10 * x);
    }

    private static double easeInOutExpo(double x) {
        return x == 0
                ? 0
                : x == 1
                ? 1
                : x < 0.5 ? Math.pow(2, 20 * x - 10) / 2
                : (2 - Math.pow(2, -20 * x + 10)) / 2;
    }

    private static double easeInCirc(double x) {
        return 1 - Math.sqrt(1 - Math.pow(x, 2));
    }

    private static double easeOutCirc(double x) {
        return Math.sqrt(1 - Math.pow(x - 1, 2));
    }

    private static double easeInOutCirc(double x) {
        return x < 0.5
                ? (1 - Math.sqrt(1 - Math.pow(2 * x, 2))) / 2
                : (Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2;
    }

    private static double easeInBack(double x) {
        double c1 = 1.70158;
        double c3 = c1 + 1;

        return c3 * x * x * x - c1 * x * x;
    }

    private static double easeOutBack(double x) {
        double c1 = 1.70158;
        double c3 = c1 + 1;

        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }

    private static double easeInOutBack(double x) {
        double c1 = 1.70158;
        double c2 = c1 * 1.525;

        return x < 0.5
                ? (Math.pow(2 * x, 2) * ((c2 + 1) * 2 * x - c2)) / 2
                : (Math.pow(2 * x - 2, 2) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2;
    }

    private static double easeInElastic(double x) {
        double c4 = (2 * Math.PI) / 3;

        return x == 0
                ? 0
                : x == 1
                ? 1
                : -Math.pow(2, 10 * x - 10) * Math.sin((x * 10 - 10.75) * c4);
    }

    private static double easeOutElastic(double x) {
        double c4 = (2 * Math.PI) / 3;

        return x == 0
                ? 0
                : x == 1
                ? 1
                : Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1;
    }

    private static double easeInOutElastic(double x) {
        double c5 = (2 * Math.PI) / 4.5;

        return x == 0
                ? 0
                : x == 1
                ? 1
                : x < 0.5
                ? -(Math.pow(2, 20 * x - 10) * Math.sin((20 * x - 11.125) * c5)) / 2
                : (Math.pow(2, -20 * x + 10) * Math.sin((20 * x - 11.125) * c5)) / 2 + 1;
    }

    private static double easeInBounce(double x) {
        return 1 - easeOutBounce(1 - x);
    }

    private static double easeOutBounce(double x) {
        double n1 = 7.5625;
        double d1 = 2.75;

        if (x < 1 / d1) {
            return n1 * x * x;
        } else if (x < 2 / d1) {
            return n1 * (x -= 1.5 / d1) * x + 0.75;
        } else if (x < 2.5 / d1) {
            return n1 * (x -= 2.25 / d1) * x + 0.9375;
        } else {
            return n1 * (x -= 2.625 / d1) * x + 0.984375;
        }
    }

    private static double easeInOutBounce(double x) {
        return x < 0.5
                ? (1 - easeOutBounce(1 - 2 * x)) / 2
                : (1 + easeOutBounce(2 * x - 1)) / 2;
    }

    public enum Easing {
        EASE_IN_SINE,
        EASE_OUT_SINE,
        EASE_IN_OUT_SINE,
        EASE_IN_QUAD,
        EASE_OUT_QUAD,
        EASE_IN_OUT_QUAD,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC,
        EASE_IN_QUART,
        EASE_OUT_QUART,
        EASE_IN_OUT_QUART,
        EASE_IN_QUINT,
        EASE_OUT_QUINT,
        EASE_IN_OUT_QUINT,
        EASE_IN_EXPO,
        EASE_OUT_EXPO,
        EASE_IN_OUT_EXPO,
        EASE_IN_CIRC,
        EASE_OUT_CIRC,
        EASE_IN_OUT_CIRC,
        EASE_IN_BACK,
        EASE_OUT_BACK,
        EASE_IN_OUT_BACK,
        EASE_IN_ELASTIC,
        EASE_OUT_ELASTIC,
        EASE_IN_OUT_ELASTIC,
        EASE_IN_BOUNCE,
        EASE_OUT_BOUNCE,
        EASE_IN_OUT_BOUNCE;

        public static Easing of(String value) {
            return valueOf(value.toUpperCase());
        }
    }
}
