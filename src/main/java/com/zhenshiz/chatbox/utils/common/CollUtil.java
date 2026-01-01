package com.zhenshiz.chatbox.utils.common;

import java.util.Collection;
import java.util.Map;

public class CollUtil {

    public static Boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean notEmpty(Object value) {
        if (value instanceof CharSequence str)  return !str.isEmpty();
        if (value instanceof Collection<?> col) return !col.isEmpty();
        if (value instanceof Map<?, ?> map)     return !map.isEmpty();
        if (value instanceof Object[] arr)      return arr.length > 0;
        return value != null;
    }
}
