package com.zhenshiz.chatbox.utils.chatbox;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

public class ScreenUtil {

    public static Window getWindow() {return Minecraft.getInstance().getWindow();}

    public static boolean hasControlDown() {
        if (Util.getPlatform() == Util.OS.OSX) {
            return InputConstants.isKeyDown(getWindow(), 343) || InputConstants.isKeyDown(getWindow(), 347);
        } else {
            return InputConstants.isKeyDown(getWindow(), 341) || InputConstants.isKeyDown(getWindow(), 345);
        }
    }

    public static boolean hasShiftDown() {
        return InputConstants.isKeyDown(getWindow(), 340) || InputConstants.isKeyDown(getWindow(), 344);
    }

    public static boolean hasAltDown() {
        return InputConstants.isKeyDown(getWindow(), 342) || InputConstants.isKeyDown(getWindow(), 346);
    }

    public static boolean isCut(int keyCode) {
        return keyCode == 88 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isPaste(int keyCode) {
        return keyCode == 86 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isCopy(int keyCode) {
        return keyCode == 67 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }

    public static boolean isSelectAll(int keyCode) {
        return keyCode == 65 && hasControlDown() && !hasShiftDown() && !hasAltDown();
    }
}
