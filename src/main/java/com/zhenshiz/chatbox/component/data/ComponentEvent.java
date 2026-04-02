package com.zhenshiz.chatbox.component.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.Portrait;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.SoundUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import com.zhenshiz.chatbox.utils.mvel.MVELUtil;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static com.zhenshiz.chatbox.api.EventExecutor.*;
import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.*;

/**
 * 对话框组件渲染事件，并非只适用于组件，亦可在一句新的对话开始时触发，此时{@link #component}为null
 */
@AllArgsConstructor
@NoArgsConstructor
public class ComponentEvent {
    public static final String ON_START = "ON_START", ON_END = "ON_END", ON_CLICK = "ON_CLICK",
            TICK = "TICK", CHECK = "CHECK",
            ON_MOUSE_OVER = "ON_MOUSE_OVER", ON_MOUSE_OUT = "ON_MOUSE_OUT", NONE = "NONE";
    public String trigger = NONE;
    public String condition = ""; // 事件触发条件，默认为空字符串，表示无条件触发；无前缀默认执行MVEL；以execute开头表示执行指令
    public String type = "";
    public String value = "";
    @Setter
    @Nullable private AbstractComponent<?> component;

    public static ComponentEvent of(ChatBoxTheme.RenderEvent e) {
        return of(e.trigger, e.condition, e.type, e.value, null);
    }
    public static ComponentEvent of(String trigger, String condition, String type, String value, @Nullable AbstractComponent<?> component) {
        return new ComponentEvent(ofTrigger(trigger), condition, type, value, component);
    }

    /**根据谓语条件触发事件，仍会验证事件本身的condition*/
    protected boolean fire(Predicate<ComponentEvent> predicate, int id, int index) {
        if (!predicate.test(this)) return false;
        if (condition.isEmpty()) return execute();
        if (condition.startsWith("execute") || condition.startsWith("server:")) {
            ChatBoxCommandUtil.simplePayloadC2S("test_condition", StrUtil.merge(condition, id, index));
            return true;
        }
        return MVELUtil.evalClient(condition, component) instanceof Boolean b && b && execute();
    }

    protected boolean execute() {return executeEvent(this.component, this.type, this.value);}

    public static String ofTrigger(String trigger) { // 增加容错处理
        String s = StrUtil.isEmpty(trigger) ? NONE : trigger.toUpperCase();
        if (s.contains("START")) return ON_START;
        if (s.contains("END")) return ON_END;
        if (s.contains("CLICK")) return ON_CLICK;
        if (s.contains("TICK")) return TICK;
        if (s.contains("CHECK")) return CHECK;
        if (s.contains("MOUSE")) {
            if (s.contains("OVER")) return ON_MOUSE_OVER;
            if (s.contains("OUT")) return ON_MOUSE_OUT;
        }
        ChatBox.LOGGER.warn("Unknown trigger: {}, available triggers: {}", trigger,
                new String[]{ON_START, ON_END, ON_CLICK, TICK, CHECK, ON_MOUSE_OVER, ON_MOUSE_OUT});
        return NONE;
    }

    public static void registerDefaultEvents() {
        registerEvent("COMMAND", (c, s) -> {}, () -> true, ComponentEvent::executeCommands);
        // 实际上是会在服务端执行的，只是不想再加一个事件类型了
        registerClientEvent("MVEL", (c, s) -> {
            if (s.startsWith("server:")) ChatBoxCommandUtil.simplePayloadC2S("test_condition", StrUtil.merge(s));
            else MVELUtil.evalClient(s, c);
        });

        registerClientEvent("JUMP", (c, next) -> { //跳转到指定的对话或者其它模块的对话
            if (next.equalsIgnoreCase("this")) return;
            if (StrUtil.isEmpty(next)) {            //跳转下一句话
                skipDialogues(dialoguesResourceLocation, group, index + 1);
            } else if (StrUtil.isInteger(next)) {   //如果为数字跳转到指定序号的对话
                int index = Integer.parseInt(next);
                skipDialogues(dialoguesResourceLocation, group, index);
            } else {                                //如果是英文则跳转到指定模块的对话
                skipDialogues(dialoguesResourceLocation, next);
            }
        });

        registerClientEvent("GOTO_NEXT", (c, s) -> chatBoxScreen.dialogBoxClick());

        registerClientEvent("PLAY_VOICE", (c, voice) -> chatBoxScreen.playVoice(voice));
        registerClientEvent("PLAY_SOUND", (c, s) -> SoundUtil.playSound(s));
        registerClientEvent("STOP_SOUND", (c, s) -> SoundUtil.stopSound(s));

        registerClientEvent("SHOW", (c, s) -> chatBoxScreen.setComponentHidden(s, false, c));
        registerClientEvent("HIDE", (c, s) -> chatBoxScreen.setComponentHidden(s, true, c));
        // 替换组件时，如果当前组件不为null，就隐藏当前组件，否则完全等价于SHOW事件
        registerClientEvent("REPLACE", (c, values) -> {
            if (c != null) c.setHidden(true);
            chatBoxScreen.setComponentHidden(values, false, null);
        });
        registerClientEvent("LOCK", (c, s) -> chatBoxScreen.setComponentLock(s, true, c));
        registerClientEvent("UNLOCK", (c, s) -> chatBoxScreen.setComponentLock(s, false, c));
        registerClientEvent("SET_NORMAL", (c, s) ->
                chatBoxScreen.getCompByDesc(s, c).forEach(AbstractComponent::setNormal));

        registerClientEvent("SET_AUTOPLAY", (c, s) -> chatBoxScreen.autoPlay = Boolean.parseBoolean(s));

        registerClientEvent("SCALE", (c, s) -> {
            if (c != null) c.setScale(Float.parseFloat(s));
        });

        registerClientEvent("RESTART_ANIMATION", (c, s) -> {
            if (c instanceof Portrait<?> portrait) portrait.restartAnimation();
        });

        registerClientEvent("PLAY_ANIMATION", (c, s) -> {
            if (c instanceof Portrait<?> portrait) portrait.setAnimationType(s);
        });
    }

    public static void executeCommands(ServerPlayer player, String value) {
        var commands = value.split(";");
        for (var command : commands) {
            command = command.trim();
            if (!command.isBlank()) executeCommand(player.server, player, command);
        }
    }

    public static int executeCommand(@NotNull MinecraftServer server, @Nullable Entity entity, String command) {
        if (entity instanceof ServerPlayer player) {
            command = ChatBoxCommandUtil.parseTargetPlaceholders(player, command);
            if (ChatBox.pluginHelper != null && !command.startsWith("execute"))
                return ChatBox.pluginHelper.executeCommand(player.getUUID(), command);
        }

        // 创建命令源，并赋予2级权限，且禁止输出
        CommandSourceStack commandSource;
        if (entity != null) commandSource = entity.createCommandSourceStack();
        else commandSource = server.createCommandSourceStack();
        commandSource = commandSource.withPermission(Commands.LEVEL_GAMEMASTERS).withSuppressedOutput();
        var dispatcher = server.getCommands().getDispatcher();
        try {
            return dispatcher.execute(dispatcher.parse(command, commandSource));
        } catch (UnsupportedOperationException e) {
            server.getCommands().performPrefixedCommand(commandSource, command);
        } catch (CommandSyntaxException e) {
            ChatBox.LOGGER.error("Error executing command on server: {}", command, e);
        }
        return 0;
    }
}
