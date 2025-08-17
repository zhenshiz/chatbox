package com.zhenshiz.chatbox.utils.common;

import java.util.Collection;

public class CollUtil {

    public static Boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
