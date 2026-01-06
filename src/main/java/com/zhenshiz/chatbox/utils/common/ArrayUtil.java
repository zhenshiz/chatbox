package com.zhenshiz.chatbox.utils.common;

import java.util.Arrays;

public class ArrayUtil {

    public static boolean isArray(Object obj) {
        return null != obj && obj.getClass().isArray();
    }

    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static String toString(Object obj) {
        if (obj instanceof long[]) return Arrays.toString((long[]) obj);
        if (obj instanceof int[]) return Arrays.toString((int[]) obj);
        if (obj instanceof short[]) return Arrays.toString((short[]) obj);
        if (obj instanceof char[]) return Arrays.toString((char[]) obj);
        if (obj instanceof byte[]) return Arrays.toString((byte[]) obj);
        if (obj instanceof boolean[]) return Arrays.toString((boolean[]) obj);
        if (obj instanceof float[]) return Arrays.toString((float[]) obj);
        if (obj instanceof double[]) return Arrays.toString((double[]) obj);

        if (isArray(obj)) {
            try {
                return Arrays.deepToString((Object[]) obj);
            } catch (Exception ignored) {}
        }
        return obj.toString();
    }
}
