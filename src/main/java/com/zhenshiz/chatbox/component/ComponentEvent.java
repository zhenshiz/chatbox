package com.zhenshiz.chatbox.component;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import com.zhenshiz.chatbox.utils.chatbox.SoundUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
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

import java.util.List;
import java.util.Objects;

import static com.zhenshiz.chatbox.api.EventExecutor.*;
import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.*;

/**
 * 对话框组件渲染事件，并非只适用于组件，亦可在一句新的对话开始时触发，此时{@link #component}为null
 */
@AllArgsConstructor
@NoArgsConstructor
public class ComponentEvent {
    public static final String ON_START = "ON_START", ON_END = "ON_END", ON_CLICK = "ON_CLICK",
            ON_MOUSE_OVER = "ON_MOUSE_OVER", ON_MOUSE_OUT = "ON_MOUSE_OUT", NONE = "NONE";
    public String trigger = NONE;
    public String type = "";
    public String value = "";
    @Setter
    @Nullable private AbstractComponent<?> component;

    public static ComponentEvent of(ChatBoxTheme.RenderEvent e) {
        return of(e.trigger, e.type, e.value, null);
    }
    public static ComponentEvent of(String trigger, String type, String value, @Nullable AbstractComponent<?> component) {
        return new ComponentEvent(ofTrigger(trigger), type, value, component);
    }

    /**
     * 根据提供的触发时机，执行组件事件
     * @return 若触发了组件事件，则返回true
     */
    public boolean fire(String trigger) {
        if (Objects.equals(this.trigger, NONE) || !Objects.equals(this.trigger, trigger)) return false;
        return executeEvent(this.component, this.type, this.value);
    }

    /**
     * 根据提供的触发时机，执行所有组件事件
     * @return 成功执行的组件事件数量
     */
    public static int fireAll(List<ComponentEvent> events, String trigger) {
        if (CollUtil.isEmpty(events)) return 0;
        trigger = ofTrigger(trigger);
        int count = 0;
        for (ComponentEvent event : events) {
            if (event.fire(trigger)) count++;
        }
        return count;
    }

    public static String ofTrigger(String trigger) { // 增加容错处理
        String s = StrUtil.isEmpty(trigger) ? NONE : trigger.toUpperCase();
        if (s.contains("START")) return ON_START;
        if (s.contains("END")) return ON_END;
        if (s.contains("CLICK")) return ON_CLICK;
        if (s.contains("MOUSE")) {
            if (s.contains("OVER")) return ON_MOUSE_OVER;
            if (s.contains("OUT")) return ON_MOUSE_OUT;
        }
        ChatBox.LOGGER.warn("Unknown trigger: {}, available triggers: {}", trigger,
                new String[]{ON_START, ON_END, ON_CLICK, ON_MOUSE_OVER, ON_MOUSE_OUT});
        return NONE;
    }

    public static void registerDefaultEvents() {
        registerEvent("COMMAND", (c, s) -> {}, () -> true, ComponentEvent::executeCommands);

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

        registerClientEvent("SHOW", (c, values) -> chatBoxScreen.setComponentHidden(values, false, c));
        registerClientEvent("HIDE", (c, values) -> chatBoxScreen.setComponentHidden(values, true, c));
        // 替换组件时，如果当前组件不为null，就隐藏当前组件，否则完全等价于SHOW事件
        registerClientEvent("REPLACE", (c, values) -> {
            if (c != null) c.setHidden(true);
            chatBoxScreen.setComponentHidden(values, false, null);
        });

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
