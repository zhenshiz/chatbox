package com.zhenshiz.chatbox.component.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.Config;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.data.ChatBoxDialoguesLoader;
import com.zhenshiz.chatbox.utils.chatbox.ChatBoxUtil;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import com.zhenshiz.chatbox.utils.mvel.MVELUtil;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Accessors(chain = true)
public class ChatBoxText {
    public static final JsonElement EMPTY = new JsonPrimitive("");
    public static final ChatBoxText EMPTY_TEXT = new ChatBoxText(EMPTY, false);
    public static final Runnable NO_OP = () -> {};
    static final Pattern RUBY_PATTERN = Pattern.compile("<ruby\\s+(\\d+)\\s+([^>]*)>");

    final JsonElement original;
    @Nullable MutableComponent component;
    @Nullable String plainText;
    @Nullable Style style;
    @Nullable List<RubyPart> rubyParts;
    public record RubyPart(int index, int chars, String ruby) {}
    // 打字机效果相关属性
    public static final String TYPEWRITER = "typewriter";
    public static final String CHAR_PER_SECOND = "charPerSecond";
    @Setter public boolean typewriter;
    boolean allOver;
    int lastRenderTick;
    @Setter public double charPerTick = Config.charPerSecond.get() / 20d;
    public double charIndex;
    @Setter public Runnable onEnd = NO_OP;

    private ChatBoxText(JsonElement original, boolean typewriter) {
        var deepCopy = original.deepCopy();
        this.original = deepCopy;
        setTypewriter(typewriter);
        MutableComponent component;
        if (deepCopy.isJsonPrimitive()) {
            component = MutableComponent.create(createLiteral(RenderUtil.translated(deepCopy.getAsString())));
        } else if (deepCopy instanceof JsonObject object) {
            if (object.has(TYPEWRITER)) setTypewriter(getAndRemove(object, TYPEWRITER).getAsBoolean());
            if (object.has(CHAR_PER_SECOND)) setCharPerTick(getAndRemove(object, CHAR_PER_SECOND).getAsDouble() / 20d);
            component = parseAndReplace(object);
        } else if (deepCopy.isJsonArray()) component = parseAndReplace(deepCopy);
        else component = Component.empty();

        if (component.getSiblings().isEmpty()) { // 如果没有子组件，则解析注音标签，保留样式和纯文本
            var s = component.getString();
            if (!s.isEmpty()) {
                this.style = component.getStyle();
                if (s.contains("<ruby ")) {
                    StringBuilder noRubyBuilder = new StringBuilder();
                    var matcher = RUBY_PATTERN.matcher(s);
                    int lastEnd = 0;
                    while (matcher.find()) {
                        int num = Integer.parseInt(matcher.group(1));
                        int end = matcher.end();
                        // 添加标签前的文本
                        noRubyBuilder.append(s, lastEnd, matcher.start());
                        // 检查是否有足够的字符用于ruby标注
                        if (end + num > s.length()) {lastEnd = end; break;}
                        String ruby = matcher.group(2);
                        rubyParts().add(new RubyPart(noRubyBuilder.length(), num, ruby));
                        lastEnd = end;
                    }
                    // 添加剩余的文本
                    if (lastEnd < s.length()) noRubyBuilder.append(s.substring(lastEnd));
                    this.plainText = noRubyBuilder.toString();
                } else this.plainText = s;
            }
        } else this.component = component;
    }

    private MutableComponent parseAndReplace(JsonElement original) {
        var component = BeanUtil.getValueOrDefault(Component.Serializer.fromJson(original, MVELUtil.getRegistry()), Component.empty());
        if (component.getSiblings().isEmpty()) forComponent(component, c ->
                c.contents = createLiteral(getStringWithoutSiblings(c)));
        else forComponent(component, c ->
                c.contents = createLiteral(ChatBoxUtil.parseText(getStringWithoutSiblings(c), true)));
        return component;
    }

    public static ChatBoxText of(JsonElement original) {return of(original, false);}
    public static ChatBoxText of(JsonElement original, boolean typewriter) {
        if (original == null || original.isJsonNull() || original.equals(EMPTY)) return EMPTY_TEXT;
        return new ChatBoxText(original, typewriter);
    }

    public static ChatBoxText of(String text) {return of(text, false);}
    public static ChatBoxText of(String text, boolean typewriter) {
        if (text == null || text.isBlank()) return EMPTY_TEXT;
        return of(ChatBoxDialoguesLoader.GSON.fromJson(text, JsonElement.class), typewriter);
    }

    public List<RubyPart> rubyParts() {
        if (this.rubyParts == null) this.rubyParts = new ArrayList<>();
        return this.rubyParts;
    }
    public RubyPart[] rubyPartArrays() {
        var empty = new RubyPart[0];
        if (this.rubyParts == null) return empty;
        return this.rubyParts.toArray(empty);
    }

    /**
     * 获取已移除标签文本{@link #plainText}指定范围内的ruby标签，用于换行时绘制
     * @param start 开始索引（包含）
     * @param end 结束索引（不包含）
     */
    public RubyPart[] rubyFromTo(int start, int end) {
        if (rubyParts == null) return new RubyPart[0];
        return rubyParts.stream().filter(part -> part.index >= start && part.index < end).map(p -> new RubyPart(p.index - start, p.chars, p.ruby)).toArray(RubyPart[]::new);
    }

    public void setAllOver(boolean allOver) {
        this.allOver = allOver;
        if (allOver) onEnd.run();
        else charIndex = 0;
    }

    public void drawText(GuiGraphics guiGraphics, int startX, int startY, int lineWidth, AbstractComponent.AlignX alignX, int color, boolean lineBreak) {
        var component = this.component; var s = this.plainText;
        if (s == null && component == null) return;
        if (component == null) component = Component.literal(ChatBoxUtil.parseText(s, lineBreak)).setStyle(this.style);
        if (!allOver) {
            if (typewriter) {
                incrementCharIndex();
                component = subComponent(component, (int) charIndex, () -> setAllOver(true));
            } else setAllOver(true); // 非打字机效果，allOver属性不会自动设置为true，需要手动设置为true
        }
        if (lineBreak) {
            int renderY = startY;
            int partStart = 0;
            var font = ChatBoxUtil.minecraft.font;
            var lines = font.getSplitter().splitLines(component, lineWidth, Style.EMPTY);
            for (var line : lines) {
                var textPart = line.getString();
                var rubyFromTo = rubyFromTo(partStart, partStart + textPart.length());
                drawStringAlign(guiGraphics, line, startX, renderY, lineWidth, alignX, color, rubyFromTo);
                if (rubyFromTo.length > 0) renderY += 6;
                renderY += font.lineHeight;
                partStart += textPart.length() + 1;
            }
        } else drawStringAlign(guiGraphics, component, startX, startY, lineWidth, alignX, color, rubyPartArrays());
    }

    private void incrementCharIndex() {
        int tick = ChatBoxUtil.chatBoxScreen.tick;
        if (tick <= lastRenderTick) return;
        lastRenderTick = tick;
        charIndex += charPerTick;
    }

    public static void drawStringAlign(GuiGraphics guiGraphics, FormattedText text, int startX, int startY, int lineWidth, AbstractComponent.AlignX alignX, int color, RubyPart... rubyParts) {
        var font = ChatBoxUtil.minecraft.font;
        int renderX = startX; int renderY = startY;
        switch (alignX) {
            case CENTER -> renderX += (lineWidth - font.width(text)) / 2;
            case RIGHT ->  renderX +=  lineWidth - font.width(text);
        }
        if (rubyParts.length > 0) {
            String string = text.getString();
            boolean rubyRendered = false;
            for (var part : rubyParts) {
                int rubyStart = part.index();
                if (rubyStart < 0 || rubyStart + part.chars() > string.length()) continue;
                rubyRendered = true;
                int rubyX = renderX + font.width(string.substring(0, rubyStart));
                int subTextWidth = font.width(string.substring(rubyStart, rubyStart + part.chars()));
                var rubyText = ChatBoxUtil.parseText(part.ruby(), false);
                int scaleX = rubyX + subTextWidth / 2;
                rubyX += (subTextWidth - font.width(rubyText)) / 2;
                var pose = guiGraphics.pose();
                pose.pushPose();
                pose.last().pose().scaleAround(0.7f, scaleX, renderY + 5, 0);
                guiGraphics.drawString(font, rubyText, rubyX, renderY - 1, color, false);
                pose.popPose();
            }
            if (rubyRendered) renderY += 6;
        }
        guiGraphics.drawString(font, Language.getInstance().getVisualOrder(text), renderX, renderY, color, false);
        ChatBoxUtil.chatBoxScreen.addRenderedText(renderX, renderY, text);
    }

    public static void forComponent(MutableComponent component, Consumer<MutableComponent> consumer) {
        consumer.accept(component);
        for (var sibling : component.getSiblings()) forComponent((MutableComponent) sibling, consumer);
    }

    private static String getStringWithoutSiblings(MutableComponent component) {
        var copy = component.copy();
        copy.getSiblings().clear();
        return copy.getString();
    }

    private static String getLiteralOrEmpty(ComponentContents contents) {
        if (contents instanceof PlainTextContents plainText) return plainText.text();
        return "";
    }

    private static PlainTextContents createLiteral(String text) {
        if (ChatBox.isModLoaded("textanimator")) text = text.replace("<typewriter>", "");
        return PlainTextContents.create(text);
    }

    private static MutableComponent copySingleWithStyle(Component component, String text) {
        if (text == null) text = getLiteralOrEmpty(component.getContents());
        return Component.literal(text).setStyle(component.getStyle());
    }

    public static MutableComponent subComponent(MutableComponent component, int endIndex, Runnable onEnd) {
        if (endIndex < 0) { onEnd.run(); return component; }
        String fullText = component.getString();
        int realLength = getRealLength(fullText);
        if (realLength <= endIndex + 1) { onEnd.run(); return component; }

        var siblings = component.getSiblings();
        if (siblings.isEmpty()) return copySingleWithStyle(component, subString(fullText, endIndex));
        else {
            var copy = component.copy();
            copy.getSiblings().clear();
            String copyText = copy.getString();
            int rootLength = getRealLength(copyText);
            if (rootLength > endIndex) return copySingleWithStyle(copy, subString(copyText, endIndex));
            else {
                int fixedEndIndex = endIndex - rootLength;
                for (var sibling : siblings) {
                    copy.append(subComponent((MutableComponent) sibling, fixedEndIndex, NO_OP));
                    int updatedLength = getRealLength(copy.getString());
                    if (updatedLength > endIndex) return copy;
                    fixedEndIndex = endIndex - updatedLength;
                }
                return copy;
            }
        }
    }

    public static String subString(String text, int endIndex) {
        if (text == null || text.isEmpty()) return "";
        if (text.length() <= endIndex || endIndex < 0) return text;
        int current = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '§') {
                i++;
                continue;
            }
            if (c == '<') {
                int closing = text.indexOf('>', i + 1);
                int another = text.indexOf('<', i + 1);
                if (another != -1 && another < closing) continue;
                if (closing != -1) {
                    i = closing;
                    continue;
                }
            }
            current++;
            if (current >= endIndex + 1) return text.substring(0, i);
        }
        return text;
    }

    public static int getRealLength(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.replaceAll("<[^<]*>", "")
                .replace("\\", "").replace("§", "").length();
    }

    public static JsonElement getAndRemove(JsonObject object, String key) {
        return BeanUtil.getValueOrDefault(object.remove(key), EMPTY);
    }

    @Override
    public String toString() {return "ChatBoxText{" + original + '}';}

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ChatBoxText that)) return false;
        return Objects.equals(original, that.original);
    }

    @Override
    public int hashCode() {return Objects.hashCode(original);}
}
