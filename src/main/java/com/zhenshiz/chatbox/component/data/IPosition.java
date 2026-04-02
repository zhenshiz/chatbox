package com.zhenshiz.chatbox.component.data;

import com.zhenshiz.chatbox.utils.chatbox.RenderUtil;

public interface IPosition {

    static float calWidth(float  value) {return RenderUtil.screenWidth()  * value / 100;}
    static float calHeight(float value) {return RenderUtil.screenHeight() * value / 100;}

    /**@return 屏幕长度百分比宽度。*/
    float getWidth();
    /**@return 屏幕宽度百分比高度。*/
    float getHeight();

    /**@return 未进行变换时的实际x坐标。*/
    float realX();
    /**@return 未进行变换时的实际y坐标。*/
    float realY();
    /**@return 未进行变换时的实际宽度。*/
    default float realWidth() {return calWidth(getWidth());}
    /**@return 未进行变换时的实际高度。*/
    default float realHeight() {return calHeight(getHeight());}

    float getScale();

    /**@return 缩放后x坐标1。*/
    default int x1() {return (int) (realX() + realWidth()  * (1 - getScale()) / 2);}
    /**@return 缩放后y坐标1。*/
    default int y1() {return (int) (realY() + realHeight() * (1 - getScale()) / 2);}
    /**@return 缩放后x坐标2。*/
    default int x2() {return (int) (realX() + realWidth()  * (1 + getScale()) / 2);}
    /**@return 缩放后y坐标2。*/
    default int y2() {return (int) (realY() + realHeight() * (1 + getScale()) / 2);}

    /**@return 鼠标位置是否在矩形范围内。*/
    default boolean isSelect(int mouseX, int mouseY) {
        return mouseX >= x1() && mouseX <= x2() && mouseY >= y1() && mouseY <= y2();
    }
}
