package com.zhenshiz.chatbox.component;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.utils.chatbox.SoundUtil;
import com.zhenshiz.chatbox.utils.common.CollUtil;
import com.zhenshiz.chatbox.utils.common.StrUtil;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.zhenshiz.chatbox.api.EventExecutor.*;
import static com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil.*;

/**
 * 对话框组件渲染事件，并非只适用于组件，亦可在一句新的对话开始时触发，此时{@link #component}为null
 */
@AllArgsConstructor
@NoArgsConstructor
public class ComponentEvent {
    public Trigger trigger = Trigger.NONE;
    public String type = "";
    public String value = "";
    @Setter
    @Nullable private AbstractComponent<?> component;

    /**
     * 根据提供的触发时机，执行组件事件
     * @return 若触发了组件事件，则返回true
     */
    public boolean fire(Trigger trigger) {
        if (this.trigger == Trigger.NONE || this.trigger != trigger) return false;
        return executeEvent(this.component, this.type, this.value);
    }

    /**
     * 根据提供的触发时机，执行所有组件事件
     * @return 成功执行的组件事件数量
     */
    public static int fireAll(List<ComponentEvent> events, Trigger trigger) {
        if (CollUtil.isEmpty(events)) return 0;
        int count = 0;
        for (ComponentEvent event : events) {
            if (event.fire(trigger)) count++;
        }
        return count;
    }

    public enum Trigger {
        ON_START,
        ON_END,
        ON_CLICK,
        ON_MOUSE_OVER,
        ON_MOUSE_OUT,
        NONE;

        public static Trigger of(String trigger) { // 增加容错处理
            String s = StrUtil.isEmpty(trigger) ? "NONE" : trigger.toUpperCase();
            if (s.contains("START") || s.contains("BEGIN")) return ON_START;
            if (s.contains("END") || s.contains("STOP")) return ON_END;
            if (s.contains("CLICK")) return ON_CLICK;
            if (s.contains("MOUSE")) {
                if (s.contains("OVER")) return ON_MOUSE_OVER;
                if (s.contains("OUT")) return ON_MOUSE_OUT;
            }
            try {
                return Trigger.valueOf(s);
            } catch (IllegalArgumentException e) {
                ChatBox.LOGGER.warn("Unknown trigger: {}, available triggers: {}", trigger, Arrays.stream(values()).limit(5).toArray());
                return NONE;
            }
        }
    }

    public static void registerDefaultEvents() {
        registerEvent("COMMAND", (c, s) -> {}, () -> true, ((player, value) -> {
            var commands = value.split(";");
            for (var command : commands) {
                command = command.trim();
                if (!command.isBlank()) executeCommand(player.server, player, command);
            }
        }));

        registerClientEvent("JUMP", (c, next) -> { //跳转到指定的对话或者其它模块的对话
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
            if (c instanceof Portrait portrait) portrait.setScale(Float.parseFloat(s));
        });
    }

    public static int executeCommand(@NotNull MinecraftServer server, @Nullable Entity entity, String command) {
        if (ChatBox.pluginHelper != null && entity instanceof Player player) {
            command = ChatBox.pluginHelper.parsePapiPlaceholders(player.getUUID(), command);
            if (!command.startsWith("execute")) return ChatBox.pluginHelper.executeCommand(player.getUUID(), command);
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
