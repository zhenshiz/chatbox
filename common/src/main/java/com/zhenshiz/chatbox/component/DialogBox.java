package com.zhenshiz.chatbox.component;

import com.zhenshiz.chatbox.ChatBox;
import com.zhenshiz.chatbox.client.ChatBoxClient;
import com.zhenshiz.chatbox.component.data.Attachment;
import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;
import com.zhenshiz.chatbox.utils.common.BeanUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class DialogBox extends Portrait<DialogBox> {
    public static final String dialog_box = "chatbox:textures/chatbox/default_dialog_box.png";
    //对话框文本
    private String text = "";
    //文本x位置
    public float textX = 0;
    //文本y位置
    public float textY = 0;
    //文本对齐方式
    public AlignX textAlign = AlignX.LEFT;
    //名称
    public String name = "";
    //名称x位置
    public float nameX = 0;
    //名称y位置
    public float nameY = 0;
    //一行文本的宽度
    public float lineWidth = 100f;

    //全部文字是否全部显示
    public boolean isAllOver;
    public int tickCount;
    private int textLength = 0;
    private int charIndex;

    @Override
    public String getId() {return "dialog_box";}

    @Override
    public String getTexture() {return BeanUtil.getValueOrDefault(super.getTexture(), dialog_box);}

    public DialogBox setText(String text) {
        if (text != null) {
            // 获取翻译键的文本
            text = RenderUtil.translated(text);
            if (ChatBox.PLATFORM.isModLoaded("textanimator")) text = text.replace("<typewriter>", "");
            this.text = text;
            this.textLength = getRealLength(parseText(text));
        }
        return this;
    }

    public DialogBox setTextAlign(String textAlign) {
        if (textAlign != null) this.textAlign = AlignX.of(textAlign);
        return this;
    }

    public DialogBox setName(String name) {
        if (name != null) this.name = name;
        return this;
    }

    public DialogBox setNamePosition(float x, float y) {
        this.nameX = x;
        this.nameY = y;
        return this;
    }

    public DialogBox setTextPosition(float x, float y) {
        this.textX = x;
        this.textY = y;
        return this;
    }

    public DialogBox setLineWidth(Float lineWidth) {
        if (checkSize(lineWidth)) this.lineWidth = lineWidth;
        return this;
    }

    public DialogBox setAllOver(boolean allOver) {
        this.isAllOver = allOver;
        // 全部文字显示完成时触发ON_END事件
        if (allOver) {
            fireEvent("ON_END");
            charIndex = -1; // 防止字符串长度随动态解析而改变，也是为了减少计算次数
        } else { // 原来的resetTickCount方法在这里了。
            this.tickCount = 0;
            this.charIndex = 0;
        }
        return this;
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

    public void tick() {
        if (!this.isAllOver) {
            //未全部加载，开始加载
            if (this.charIndex >= this.textLength - 1) {
                setAllOver(true);
                return;
            }
            this.tickCount++;
            float charPerTick = ChatBoxClient.conf.charPerSecond / 20f;
            if (tickCount * charPerTick >= charIndex + 1) {
                charIndex = Math.min((int) (tickCount * charPerTick), this.textLength - 1);
            }
        }
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        renderInner(mouseX, mouseY);
        //chatBox image
        renderImage(guiGraphics, getRenderResource(), addTempAttachment(
                Attachment.ofText(this.name, this.nameX, this.nameY, this.lineWidth, this.textAlign.name(), -1, false, true),
                Attachment.ofText(subString(parseText(this.text), charIndex), this.textX, this.textY, this.lineWidth, this.textAlign.name(), -1, true, false)
        ));
    }
}
