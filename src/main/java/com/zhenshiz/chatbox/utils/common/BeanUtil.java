package com.zhenshiz.chatbox.utils.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BeanUtil {
    /**
     * 返回源对象，若为null返回默认值
     *
     * @param param        源对象
     * @param defaultValue 默认值
     * @param <T>          对象类型
     * @return 源对象或默认值
     */
    public static <T> T getValueOrDefault(T param, T defaultValue) {
        return Optional.ofNullable(param).orElse(defaultValue);
    }

    /**
     * 复制源对象属性到目标对象 null值不覆盖
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyPropertiesIfTargetNull(Object source, Object target) {
        copyProperties(source, target, true);
    }

    /**
     * 复制源对象属性到目标对象 null值覆盖
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target) {
        copyProperties(source, target, false);
    }

    /**
     * 复制源对象属性到目标对象
     *
     * @param source           源对象
     * @param target           目标对象
     * @param onlyIfTargetNull 是否仅在目标属性为null时复制
     */
    private static void copyProperties(Object source, Object target, boolean onlyIfTargetNull) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target objects cannot be null");
        }

        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();

        Set<Field> sourceFields = getAllFields(sourceClass);
        Set<Field> targetFields = getAllFields(targetClass);

        for (Field targetField : targetFields) {
            try {
                // 跳过静态和final字段
                int modifiers = targetField.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                    continue;
                }

                // 使目标字段可访问
                targetField.setAccessible(true);

                // 检查是否需要在目标非空时跳过
                if (onlyIfTargetNull) {
                    Object targetValue = targetField.get(target);
                    if (targetValue != null) {
                        continue; // 目标字段已有值，跳过复制
                    }
                }

                // 查找源对象中的同名字段
                Field sourceField = findField(sourceFields, targetField.getName());
                if (sourceField == null) {
                    continue; // 源对象没有对应字段
                }

                // 使源字段可访问
                sourceField.setAccessible(true);

                // 获取源字段值
                Object sourceValue = sourceField.get(source);
                if (sourceValue == null) {
                    continue; // 源值为空，跳过
                }

                // 检查类型兼容性
                Class<?> sourceType = sourceValue.getClass();
                Class<?> targetType = targetField.getType();

                if (isCompatibleType(sourceType, targetType)) {
                    targetField.set(target, sourceValue);
                }
                // 处理基本类型与包装类型的兼容
                else if (isWrapperType(sourceType, targetType)) {
                    targetField.set(target, sourceValue);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error copying property: " + targetField.getName(), e);
            }
        }
    }

    /**
     * 获取类及其所有父类的字段
     */
    private static Set<Field> getAllFields(Class<?> clazz) {
        Set<Field> fields = new HashSet<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * 在字段集合中按名称查找字段
     */
    private static Field findField(Set<Field> fields, String name) {
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }

    /**
     * 检查类型是否兼容（相同类型或子类）
     */
    private static boolean isCompatibleType(Class<?> sourceType, Class<?> targetType) {
        return targetType.isAssignableFrom(sourceType);
    }

    /**
     * 检查包装类型与基本类型的兼容性
     */
    private static boolean isWrapperType(Class<?> sourceType, Class<?> targetType) {
        if (sourceType.isPrimitive() || targetType.isPrimitive()) {
            Class<?> wrapper;
            Class<?> primitive;

            if (sourceType.isPrimitive()) {
                primitive = sourceType;
                wrapper = getWrapperType(primitive);
                return wrapper == targetType;
            } else {
                primitive = targetType;
                wrapper = sourceType;
                return getWrapperType(primitive) == wrapper;
            }
        }
        return false;
    }

    /**
     * 获取基本类型的包装类型
     */
    private static Class<?> getWrapperType(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == double.class) return Double.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == short.class) return Short.class;
        return null;
    }
}
