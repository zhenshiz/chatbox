package com.zhenshiz.chatbox.utils.chatbox;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderUtil {
    // 属性解析器映射
    private static final Map<String, Function<Entity, String>> PROPERTY_RESOLVERS = new HashMap<>();

    public static void addPropertyResolver(String key, Function<Entity, String> resolver) {
        PROPERTY_RESOLVERS.put(key, resolver);
    }

    static {
        addPropertyResolver("name", entity -> entity.getDisplayName().getString());
        addPropertyResolver("uuid", entity -> entity.getUUID().toString());
        addPropertyResolver("tags", entity -> String.join(", ", entity.getTags()));
        addPropertyResolver("health", entity -> {
            if (entity instanceof LivingEntity livingEntity) return String.valueOf(livingEntity.getHealth());
            return "0";
        });
    }

    public static String parseTargetPlaceholders(List<Entity> chatTargets, String input) {
        if (chatTargets.isEmpty()) return input;
        // 匹配 <targetN.property> 或 <targetN> 格式的占位符
        Pattern pattern = Pattern.compile("<target(\\d+)(\\.(\\w+))?>");
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;

        while (matcher.find()) {
            // 追加匹配前的文本
            sb.append(input, lastIndex, matcher.start());
            lastIndex = matcher.end();
            try {
                // 获取目标索引
                int index = Integer.parseInt(matcher.group(1)) - 1; // 转换为0-based索引
                // 获取属性名，如果没有指定则默认为"name"
                String property = matcher.group(3) != null ? matcher.group(3) : "name";
                // 检查索引是否有效
                if (index >= 0 && index < chatTargets.size()) {
                    Entity target = chatTargets.get(index);
                    // 获取属性解析器
                    Function<Entity, String> resolver = PROPERTY_RESOLVERS.getOrDefault(property, null);
                    if (resolver != null) {
                        // 应用解析器获取属性值
                        sb.append(resolver.apply(target));
                    } else {
                        // 如果没有对应的解析器，保留占位符
                        sb.append(matcher.group());
                    }
                } else {
                    // 如果索引无效，保留占位符
                    sb.append(matcher.group());
                }
            } catch (NumberFormatException e) {
                // 如果解析索引失败，保留占位符
                sb.append(matcher.group());
            }
        }
        // 追加剩余文本
        sb.append(input.substring(lastIndex));
        return sb.toString();
    }
}
