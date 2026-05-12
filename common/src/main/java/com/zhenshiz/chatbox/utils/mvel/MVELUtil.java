package com.zhenshiz.chatbox.utils.mvel;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.scores.ScoreHolder;
import org.jetbrains.annotations.Nullable;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MVELUtil {
    private static final Map<String, Object> defaultVars = Map.of("random", new Random());
    private static final ParserContext ctx = new ParserContext();
    private static final Map<String, Serializable> compiledCache = new HashMap<>();

    @FunctionalInterface
    public interface DynamicMethod {
        Object invoke(Object o, Object... args);
    }
    @FunctionalInterface
    public interface DynamicProperty {
        Object get(Object o);
    }

    private static final Map<String, DynamicMethod> dynamicMethods = new ConcurrentHashMap<>();
    private static final Map<String, DynamicProperty> dynamicProperties = new ConcurrentHashMap<>();

    public static void registerMethod(String name, DynamicMethod handler) {
        dynamicMethods.computeIfAbsent(name, h -> handler);
    }
    public static void registerProperty(String name, DynamicProperty handler) {
        dynamicProperties.computeIfAbsent(name, h -> handler);
    }
    public static void addPropertyResolver(String key, Function<Entity, Object> resolver) {
        registerProperty(key, o -> o instanceof Entity e ? resolver.apply(e) : null);
    }

    public static void init() {
        ctx.addImport(Math.class);
        ctx.addImport("ChatBoxUtil", ChatBoxCommandUtil.class);
        addMethodInClass(MVELUtil.class);

        registerMethod("hasItem", MVELUtil::hasItem);
        registerMethod("getItemCount", MVELUtil::getItemCount);
        registerMethod("getItemBySlot", MVELUtil::getItemBySlot);
        registerMethod("getScore", MVELUtil::getScore);
        registerMethod("hasTag", MVELUtil::hasTag);
        registerMethod("tell", MVELUtil::tell);
        registerMethod("getEnchantLevel", MVELUtil::getEnchantLevel);
        registerMethod("enchant", MVELUtil::enchant);

        registerProperty("name", MVELUtil::getName);
        registerProperty("id", MVELUtil::getId);
        addPropertyResolver("uuid", Entity::getUUID);
        addPropertyResolver("foodLevel", entity -> entity instanceof Player p ? p.getFoodData().getFoodLevel() : 0);
        addPropertyResolver("offHandItem", e -> e instanceof LivingEntity le ? le.getOffhandItem() : null);
/*        registerProperty("mainHandItem", MVELUtil::getMainHandItem);
        registerProperty("count", MVELUtil::getCount);
        addPropertyResolver("tags", Entity::entityTags);
        addPropertyResolver("health", entity -> entity instanceof LivingEntity le ? le.getHealth() : 0);
        addPropertyResolver("experienceLevel", entity -> entity instanceof Player p ? p.experienceLevel : 0);
        addPropertyResolver("x", Entity::getX);
        addPropertyResolver("y", Entity::getY);
        addPropertyResolver("z", Entity::getZ);*/
    }

    /**将一个类中所有的public static方法导入到ctx中，重名的方法无法添加，不建议使用*/
    public static void addMethodInClass(Class<?> clazz) {
        for (var method : clazz.getMethods()) {
            String name = method.getName();
            if (!ctx.hasImport(name) && method.isAnnotationPresent(ChatBoxMvel.class) && Modifier.isStatic(method.getModifiers())) {
                ctx.addImport(name, method);
            }
        }
    }

    public static @Nullable Object evalClient(String expression, Object thisObj) {
        return eval(ChatBoxUtil.getPlayer(), expression, thisObj);
    }
    public static @Nullable Object eval(Player player, String expression, Object thisObj) {
        return eval(player, expression, thisObj, false);
    }
    public static @Nullable Object eval(Player player, String expression, Object thisObj, boolean log) {
        if (expression.startsWith("server:")) expression = expression.substring(7).trim();
        String expr = expression;
        Map<String, Object> vars = new HashMap<>(defaultVars);
        if (player != null) {
            vars.put("player", player);
            vars.put("gameTime", player.level().getGameTime());
            if (player.level().isClientSide()) {
                vars.put("chatbox", ChatBoxUtil.class);
                vars.put("chatboxScreen", ChatBoxUtil.chatBoxScreen);
                vars.put("chatboxTick", ChatBoxUtil.chatBoxScreen.tick);
                vars.put("targets", ChatBoxUtil.chatTargets);
            } else vars.put("targets", ChatBoxCommandUtil.serverGetChatTargets((ServerPlayer) player));
        }
        if (thisObj != null) vars.put("_this", thisObj);
        try {
            var serializable = compiledCache.computeIfAbsent(expr, s -> {
                String transform = MVELTransformer.safeTransform(replaceTarget(expr), dynamicMethods, dynamicProperties);
                return MVEL.compileExpression(transform, ctx);
            });
            if (compiledCache.size() > 721) compiledCache.clear(); // 避免过度占用内存，简单粗暴的清理策略
            return MVEL.executeExpression(serializable, vars);
        } catch (Exception e) {
            if (log) ChatBox.LOGGER.error("Failed to evaluate MVEL expression: {}", expression, e);
            return null;
        }
    }

    public static String parseTargetPlaceholders(Player player, String input) {
        Pattern pattern = Pattern.compile("<(?:target|player)[^<>]*>");
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        while (matcher.find()) {
            // 追加匹配前的文本
            sb.append(input, lastIndex, matcher.start());
            lastIndex = matcher.end();
            String placeholder = matcher.group();
            String content = placeholder.substring(1, placeholder.length() - 1); // 去掉尖括号
            if (content.equals("player") || content.equals("target") ||
                    content.startsWith("target") && StrUtil.isInteger(content.substring(6))) {
                content = content + ".name"; // 默认解析 name 属性
            }
            Object parsed = eval(player, content, null);
            if (parsed != null) sb.append(parsed);
            else sb.append(placeholder); // 无法解析时保留原占位符
        }
        // 追加剩余文本
        sb.append(input.substring(lastIndex));
        return sb.toString();
    }

    public static void commandTest(Player player, String expression) {
        var o = eval(player, expression, null, true);
        if (player != null) player.sendSystemMessage(Component.translatable("MVEL test: ").append(
                Component.literal(o == null ? "null" : o.toString())));
    }

    private static String replaceTarget(String expression) {
        if (!expression.contains("target")) return expression;
        Pattern p = Pattern.compile("\\btarget(\\d+)?\\b");
        Matcher m = p.matcher(expression);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            if (m.group(1) != null) {
                int num = Integer.parseInt(m.group(1)) - 1;
                m.appendReplacement(sb, "targets[" + num + "]");
            } else m.appendReplacement(sb, "targets[0]");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @ChatBoxMvel
    public static String getName(Object o) {
        if (o instanceof Entity entity) return entity.getDisplayName().getString();
        else if (o instanceof Item item) return new ItemStack(item).getDisplayName().getString();
        else if (o instanceof ItemStack stack) return stack.getDisplayName().getString();
        return String.valueOf(o);
    }

    @ChatBoxMvel
    public static ItemStack getItemBySlot(Object o, Object... args) {
        if (o instanceof LivingEntity le && args.length == 1 && args[0] instanceof String slot) {
            switch (slot.toLowerCase()) {
                case "head", "helmet" ->        { return le.getItemBySlot(EquipmentSlot.HEAD); }
                case "chest", "chestplate" ->   { return le.getItemBySlot(EquipmentSlot.CHEST); }
                case "legs", "leggings" ->      { return le.getItemBySlot(EquipmentSlot.LEGS); }
                case "feet", "boots" ->         { return le.getItemBySlot(EquipmentSlot.FEET); }
            }
        }
        if (o instanceof Player player && args.length == 1 && args[0] instanceof Integer slot) {
            return player.getInventory().getItem(slot);
        }
        return null;
    }

    public static boolean isServerSide() {
        if (!ChatBox.PLATFORM.isClient()) return true;
        var server = ChatBox.server;
        return server != null && server.isSameThread();
    }

    public static @Nullable RegistryAccess getRegistry() {
        if (isServerSide()) {
            var server = ChatBox.server;
            if (server != null) return server.registryAccess();
        } else if (ChatBoxUtil.getLevel() != null) return ChatBoxUtil.getLevel().registryAccess();
        return null;
    }

    public static @Nullable Holder<Enchantment> getEnchantment(String id) {
        var registry = getRegistry();
        return registry != null ? registry.lookupOrThrow(Registries.ENCHANTMENT).get(ChatBox.parseId(id)).orElse(null) : null;
    }

    @ChatBoxMvel
    public static Integer getEnchantLevel(Object o, Object... args) {
        if (o instanceof ItemStack stack && args.length == 1 && args[0] instanceof String id) {
            var holder = getEnchantment(id);
            return holder != null ? EnchantmentHelper.getItemEnchantmentLevel(holder, stack) : null;
        }
        return null;
    }

    @ChatBoxMvel
    public static String enchant(Object o, Object... args) {
        if (o instanceof ItemStack stack && args.length == 2 && args[0] instanceof String id && args[1] instanceof Integer level) {
            var holder = getEnchantment(id);
            if (holder == null) return "Enchantment " + id + " not found!";
            stack.enchant(holder, level);
            return "Success";
        }
        return "Wrong args";
    }

    @ChatBoxMvel
    public static String getId(Object o) {
        if (o instanceof Entity entity) return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        else if (o instanceof Item item) return BuiltInRegistries.ITEM.getKey(item).toString();
        else if (o instanceof ItemStack stack) return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        else if (o instanceof Block block) return BuiltInRegistries.BLOCK.getKey(block).toString();
        else if (o instanceof MobEffect effect) return BuiltInRegistries.MOB_EFFECT.getKey(effect).toString();
        else if (o instanceof AbstractComponent<?> c) return c.getId();
        return null;
    }

    public static Item getItemById(String id) {return BuiltInRegistries.ITEM.getValue(ChatBox.parseId(id));}

    @ChatBoxMvel
    public static boolean hasItem(Object o, Object... args) {return getItemCount(o, args) > 0;}

    @ChatBoxMvel
    public static int getItemCount(Object o, Object... args) {
        int count = 0;
        if (o instanceof Player player && args.length == 1 && args[0] instanceof String id) {
            for (var stack : player.getInventory()) {
                if (!stack.isEmpty() && stack.is(getItemById(id))) count += stack.getCount();
            }
        }
        return count;
    }

    @ChatBoxMvel
    public static Integer getScore(Object o, Object... args) {
        if (o instanceof Player player && args.length == 1 && args[0] instanceof String objective) {
            var scoreboard = player.level().getScoreboard();
            var obj = scoreboard.getObjective(objective);
            if (obj == null) return null;
            var score = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly(player.getScoreboardName()), obj);
            return score != null ? score.value() : 0;
        }
        return null;
    }

    @ChatBoxMvel
    public static boolean hasTag(Object o, Object... args) {
        return o instanceof Entity entity && args.length == 1 && args[0] instanceof String tag &&
                entity.entityTags().contains(tag);
    }

    @ChatBoxMvel
    public static boolean tell(Object o, Object... args) {
        if (o instanceof Player player && args.length >= 1 && args[0] instanceof String message) {
            boolean actionBar = false;
            if (args.length >= 2 && args[1] instanceof Boolean b) actionBar = b;
            var text = Component.translatable(message);
            if (actionBar) player.sendOverlayMessage(text); else player.sendSystemMessage(text);
            return true;
        }
        return false;
    }

    @ChatBoxMvel
    public static Object m(String methodName, Object target, Object... args) {
        var h = dynamicMethods.get(methodName);
        if (h != null) return h.invoke(target, args);
        throw new RuntimeException("No dynamic method registered: " + methodName);
    }

    @ChatBoxMvel
    public static Object p(String propName, Object target) {
        var h = dynamicProperties.get(propName);
        if (h != null) return h.get(target);
        throw new RuntimeException("No dynamic property registered: " + propName);
    }
}
