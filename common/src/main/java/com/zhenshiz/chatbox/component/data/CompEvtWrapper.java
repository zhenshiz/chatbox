package com.zhenshiz.chatbox.component.data;

import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.data.ChatBoxTheme;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 组件事件包装类，用于管理一个组件的多个事件，并提供触发和执行事件的方法
 * 目前只有一个列表属性events，用于存储组件事件；一个id属性用于标识事件列表
 * id的作用是当设置了需要在服务端验证条件的组件事件被触发时，客户端会将该事件的条件，id和index发给服务端以验证，若测试通过，服务端会把id和index发回客户端以执行对应的组件事件
 */
public class CompEvtWrapper {
    private static int idCounter = 0;
    @Getter
    private final List<ComponentEvent> events = new ArrayList<>();
    private int id;

    private void newId() {this.id = idCounter++;}

    public CompEvtWrapper() {newId();}

    public void clear() {
        this.events.clear();
        newId();
    }

    public CompEvtWrapper add(ComponentEvent event, AbstractComponent<?> component) {
        if (component != null) event.setComponent(component); // 有必要的 nonnull 检查
        this.events.add(event);
        return this;
    }
    public CompEvtWrapper add(ComponentEvent event) {return add(event, null);}

    public CompEvtWrapper add(String trigger, String condition, String type, String value, AbstractComponent<?> component) {
        return add(ComponentEvent.of(trigger, condition, type, value, component));
    }
    public CompEvtWrapper add(String trigger, String type, String value, AbstractComponent<?> component) {
        return add(trigger, "", type, value, component);
    }

    public CompEvtWrapper add(ChatBoxTheme.RenderEvent event, AbstractComponent<?> component) {
        return add(ComponentEvent.of(event), component);
    }

    public CompEvtWrapper set(List<ComponentEvent> events, AbstractComponent<?> component) {
        clear();
        if (component != null) for (var event : events) event.setComponent(component);
        this.events.addAll(events);
        return this;
    }

    /**
     * 根据提供的谓语条件执行组件事件，仍会验证事件本身的condition
     * @return 成功执行的组件事件数量
     */
    public int fireAll(Predicate<ComponentEvent> predicate) {
        if (events.isEmpty()) return 0;
        int count = 0;
        for (var event : events) if (event.fire(predicate, id, events.indexOf(event))) count++;
        return count;
    }

    public static Predicate<ComponentEvent> isTrigger(String trigger) {
        return c -> !c.trigger.equals(ComponentEvent.NONE) && c.trigger.equals(ComponentEvent.ofTrigger(trigger));
    }

    /**
     * 根据提供的触发时机，执行所有组件事件
     * @return 成功执行的组件事件数量
     */
    public int fireAll(String trigger) {
        if (events.isEmpty()) return 0;
        return fireAll(isTrigger(trigger));
    }

    public boolean execute(int id, int index) {
        if (events.isEmpty() || this.id != id || index < 0 || index >= events.size()) return false;
        events.get(index).execute();
        return true;
    }
}
