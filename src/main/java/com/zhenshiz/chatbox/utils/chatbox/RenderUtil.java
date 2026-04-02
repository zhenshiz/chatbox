package com.zhenshiz.chatbox.utils.chatbox;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.zhenshiz.chatbox.component.AbstractComponent;
import com.zhenshiz.chatbox.component.data.Attachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.platform.Lighting;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//? >= 1.21 {
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import java.util.concurrent.ExecutionException;
//?} else {
/*import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
*///?}

public class RenderUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();

    public static int screenWidth() {
        return minecraft.getWindow().getGuiScaledWidth();
    }

    public static int screenHeight() {
        return minecraft.getWindow().getGuiScaledHeight();
    }

    //盒子
    public static void drawBox(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        drawLine(guiGraphics, x, y, x + w, y, color);
        drawLine(guiGraphics, x, y + h, x + w, y + h, color);
        drawLine(guiGraphics, x, y, x, y + h, color);
        drawLine(guiGraphics, x + w, y, x + w, y + h, color);
    }

    //一条线
    public static void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        //? >= 1.21 {
        BufferBuilder buf = getTesselator().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        buf.addVertex(mat, (float) x1, (float) y1, 0).setColor(color);
        buf.addVertex(mat, (float) x2, (float) y2, 0).setColor(color);
        //?} else {
        /*BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = guiGraphics.pose().last().pose();

        buf.vertex(mat, (float) x1, (float) y1, 0).color(color).endVertex();
        buf.vertex(mat, (float) x2, (float) y2, 0).color(color).endVertex();
        *///?}

        beginRendering();
        drawBuffer(buf);
        finishRendering();
    }

    // image
    public static void renderImageInner(GuiGraphics guiGraphics, ResourceLocation resourceLocation, float x, float y, float uw, float uh, float width, float height) {
        //? >= 1.21 {
        BufferBuilder bufferBuilder = getTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        bufferBuilder.addVertex(matrix4f, x, y, 0).setUv(0, 0);
        bufferBuilder.addVertex(matrix4f, x, y + height, 0).setUv(0, uh);
        bufferBuilder.addVertex(matrix4f, x + width, y + height, 0).setUv(uw, uh);
        bufferBuilder.addVertex(matrix4f, x + width, y, 0).setUv(uw, 0);

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, resourceLocation);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.disableBlend();
        //?} else {
        /*BufferBuilder buf = getBuffer();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        buf.vertex(matrix4f, x, y, 0).uv(0, 0).endVertex();
        buf.vertex(matrix4f, x, y + height, 0).uv(0, uh).endVertex();
        buf.vertex(matrix4f, x + width, y + height, 0).uv(uw, uh).endVertex();
        buf.vertex(matrix4f, x + width, y, 0).uv(uw, 0).endVertex();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, resourceLocation);
        RenderSystem.enableBlend();
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.disableBlend();
        *///?}
    }

    public static void renderImage(GuiGraphics guiGraphics, ResourceLocation resourceLocation, float x, float y, float width, float height, float scale, float angle, Attachment... attachments) {
        guiGraphics.pose().pushPose();
        float centerX = x + width / 2;
        float centerY = y + height / 2;
        // 应用旋转
        guiGraphics.pose().rotateAround(new Quaternionf().fromAxisAngleDeg(0, 0, 1, angle), centerX, centerY, 0);
        guiGraphics.pose().last().pose().scaleAround(scale, centerX, centerY, 0);
        if (resourceLocation != null) renderImageInner(guiGraphics, resourceLocation, x, y, 1, 1, width, height);
        for (var attachment : attachments) attachment.render(guiGraphics, x, y);
        guiGraphics.pose().popPose();
    }

    public static void renderPlayerHead(GuiGraphics guiGraphics, String input, int x, int y, int size, float scale, float angle, Attachment... attachments) {
        PoseStack pose = guiGraphics.pose();
        ResourceLocation skin = getSkin(input)/*? >= 1.21 {*/.texture()/*?}*/;
        pose.pushPose();
        float centerX = x + (float) size / 2;
        float centerY = y + (float) size / 2;
        // 应用旋转
        guiGraphics.pose().rotateAround(new Quaternionf().fromAxisAngleDeg(0, 0, 1, angle), centerX, centerY, 0);
        guiGraphics.pose().last().pose().scaleAround(scale, centerX, centerY, 0);
        guiGraphics.blit(skin, x, y, size, size, 8, 8, 8, 8, 64, 64);
        guiGraphics.blit(skin, x - 1, y - 1, size + 2, size + 2, 40, 8, 8, 8, 64, 64);
        for (var attachment : attachments) attachment.render(guiGraphics, x, y);
        pose.popPose();
    }

    public static void renderItem(GuiGraphics guiGraphics, ItemStack item, int x, int y, float scale, float angle, Attachment... attachments) {
        guiGraphics.pose().pushPose();
        float centerX = x + 8f;
        float centerY = y + 8f;
        // 应用旋转
        guiGraphics.pose().rotateAround(new Quaternionf().fromAxisAngleDeg(0, 0, 1, angle), centerX, centerY, 0);
        guiGraphics.pose().last().pose().scaleAround(scale, centerX, centerY, 0);
        guiGraphics.renderItem(item, x, y);
        guiGraphics.renderItemDecorations(minecraft.font, item, x, y);
        for (var attachment : attachments) attachment.render(guiGraphics, x, y);
        guiGraphics.pose().popPose();
    }

    //text
    public static void drawStringAlign(GuiGraphics guiGraphics, String text, int startX, int startY, int lineWidth, AbstractComponent.AlignX alignX, int color, boolean lineBreak) {
        var withRuby = new StringWithRuby(text);
        if (lineBreak) withRuby.drawLineBreak(guiGraphics, startX, startY, lineWidth, alignX, color);
        else drawStringAlign(guiGraphics, Component.nullToEmpty(withRuby.noRuby), startX, startY, lineWidth, alignX, color, withRuby.rubyPartArrays());
    }

    public static void drawStringAlign(GuiGraphics guiGraphics, FormattedText text, int startX, int startY, int lineWidth, AbstractComponent.AlignX alignX, int color, RubyPart... rubyParts) {
        var font = minecraft.font;
        int renderX = startX; int renderY = startY;
        switch (alignX) {
            case CENTER -> renderX += (lineWidth - font.width(text)) / 2;
            case RIGHT ->  renderX +=  lineWidth - font.width(text);
        }
        if (rubyParts.length > 0) {
            String string = text.getString();
            for (var part : rubyParts) {
                int rubyStart = part.index();
                if (rubyStart < 0 || rubyStart + part.chars() > string.length()) continue;
                int rubyX = renderX + font.width(Component.nullToEmpty(string.substring(0, rubyStart)).getVisualOrderText());
                int subTextWidth = font.width(Component.nullToEmpty(string.substring(rubyStart, rubyStart + part.chars())).getVisualOrderText());
                Component rubyComponent = Component.nullToEmpty(part.ruby());
                int scaleX = rubyX + subTextWidth / 2;
                rubyX += (subTextWidth - font.width(rubyComponent.getVisualOrderText())) / 2;
                var pose = guiGraphics.pose();
                pose.pushPose();
                pose.last().pose().scaleAround(0.7f, scaleX, renderY + 5, 0);
                guiGraphics.drawString(font, rubyComponent, rubyX, renderY - 1, color, false);
                pose.popPose();
            }
            renderY += 6;
        }
        guiGraphics.drawString(font, Language.getInstance().getVisualOrder(text), renderX, renderY, color, false);
    }

    public record RubyPart(int index, int chars, String ruby) {}

    static class StringWithRuby {
        static final Pattern RUBY_PATTERN = Pattern.compile("<ruby\\s+(\\d+)\\s+([^>]*)>");
        final String raw;
        final List<RubyPart> rubyParts = new ArrayList<>();
        final boolean hasRuby;
        final String noRuby;

        public StringWithRuby(String raw) {
            this.raw = raw;
            if (raw.contains("<ruby ")) {
                StringBuilder noRubyBuilder = new StringBuilder();
                Matcher matcher = RUBY_PATTERN.matcher(raw);
                int lastEnd = 0;
                while (matcher.find()) {
                    int num = Integer.parseInt(matcher.group(1));
                    int end = matcher.end();
                    // 添加标签前的文本
                    noRubyBuilder.append(raw, lastEnd, matcher.start());
                    // 检查是否有足够的字符用于ruby标注
                    if (end + num > raw.length()) {lastEnd = end; break;}
                    String ruby = matcher.group(2);
                    rubyParts.add(new RubyPart(noRubyBuilder.length(), num, ruby));
                    lastEnd = end;
                }
                // 添加剩余的文本
                if (lastEnd < raw.length()) noRubyBuilder.append(raw.substring(lastEnd));
                this.hasRuby = !rubyParts.isEmpty();
                this.noRuby = noRubyBuilder.toString();
            } else {
                this.hasRuby = false;
                this.noRuby = raw;
            }
        }

        public RubyPart[] rubyPartArrays() {return rubyParts.toArray(new RubyPart[0]);}

        /**
         * 获取已移除标签文本{@link #noRuby}指定范围内的ruby标签，用于换行时绘制
         * @param start 开始索引（包含）
         * @param end 结束索引（不包含）
         */
        public RubyPart[] rubyFromTo(int start, int end) {
            if (!hasRuby) return new RubyPart[0];
            return rubyParts.stream().filter(part -> part.index >= start && part.index < end).map(p -> new RubyPart(p.index - start, p.chars, p.ruby)).toArray(RubyPart[]::new);
        }

        public void drawLineBreak(GuiGraphics guiGraphics, int startX, int startY, int lineWidth, AbstractComponent.AlignX alignX, int color) {
            var font = minecraft.font;
            int renderY = startY;
            int partStart = 0;
            for (var part : font.getSplitter().splitLines(Component.nullToEmpty(noRuby), lineWidth, Style.EMPTY)) {
                String textPart = part.getString();
                RubyPart[] rubyFromTo = rubyFromTo(partStart, partStart + textPart.length());
                drawStringAlign(guiGraphics, part, startX, renderY, lineWidth, alignX, color, rubyFromTo);
                if (rubyFromTo.length > 0) renderY += 6;
                renderY += font.lineHeight;
                partStart += textPart.length() + 1;
            }
        }
    }

    public static String translated(String key) {return Language.getInstance().getOrDefault(key);}

    //util

    public static void renderOpacity(GuiGraphics guiGraphics, float brightness, float opacity, Runnable runnable) {
        RenderSystem.enableBlend();
        brightness = brightness / 100f;
        guiGraphics.setColor(brightness, brightness, brightness, opacity / 100f);
        runnable.run();
        RenderSystem.disableBlend();
    }

    //private

    private static void drawBuffer(BufferBuilder buf) {
        //? >= 1.21 {
        BufferUploader.drawWithShader(Objects.requireNonNull(buf.buildOrThrow()));
        //?} else {
        /*BufferUploader.drawWithShader(buf.end());
        *///?}
    }

    public static void beginRendering() {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    public static void finishRendering() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }

    //? >= 1.21 {
    private static Tesselator getTesselator() {
        return Tesselator.getInstance();
    }
    //?} else {
    /*private static BufferBuilder getBuffer() {
        return Tesselator.getInstance().getBuilder();
    }*///?}

    //? >= 1.21 {
    private static final Map<String, PlayerSkin> skins = new HashMap<>();

    private static void handleGameProfileAsync(String input) {
        ResolvableProfile component = createProfileComponent(input);
        component.resolve()
                .thenApplyAsync(result -> {
                    GameProfile profile = result.gameProfile();
                    try {
                        PlayerSkin playerSkin = minecraft.getSkinManager().getOrLoad(profile).get();
                        skins.put(input, playerSkin);
                    } catch (InterruptedException | ExecutionException ignored) {}
                    return profile;
                })
                .exceptionally(ex -> null);
    }

    private static ResolvableProfile createProfileComponent(String input) {
        try {
            UUID uuid = UUID.fromString(input);
            return new ResolvableProfile(Optional.empty(), Optional.of(uuid), new PropertyMap());
        } catch (IllegalArgumentException e) {
            return new ResolvableProfile(Optional.of(input), Optional.empty(), new PropertyMap());
        }
    }

    private static PlayerSkin getSkin(String input) {
        if (skins.containsKey(input)) return skins.get(input);
        handleGameProfileAsync(input);
        if (skins.containsKey(input)) return skins.get(input);
        return DefaultPlayerSkin.get(minecraft.getUser().getProfileId());
    }
    //?} else {
    /*private static GameProfile createProfileComponent(String input) {
        try {
            return new GameProfile(UUID.fromString(input), null);
        } catch (IllegalArgumentException e) {
            return new GameProfile(null, input);
        }
    }

    private static final Map<String, ResourceLocation> skins = new HashMap<>();

    private static ResourceLocation getSkin(String input) {
        if (skins.containsKey(input)) return skins.get(input);
        // 尝试获取皮肤，并缓存到map中
        GameProfile profile = createProfileComponent(input);
        SkullBlockEntity.updateGameprofile(profile, gameProfile -> {
            SkinManager manager = minecraft.getSkinManager();
            var map = manager.getInsecureSkinInformation(gameProfile);
            if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) skins.put(input, manager.registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN));
        });
        if (skins.containsKey(input)) return skins.get(input);
        return DefaultPlayerSkin.getDefaultSkin(Objects.requireNonNull(minecraft.getUser().getProfileId()));
    }
    *///?}

    public static void renderEntityFollowsMouse(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int scale, float yOffset, float mouseX, float mouseY, LivingEntity entity) {
        float f = (float)(x1 + x2) / 2.0F;
        float g = (float)(y1 + y2) / 2.0F;
        guiGraphics.enableScissor(x1, y1, x2, y2);
        float h = (float)Math.atan((f - mouseX) / 40.0F);
        float i = (float)Math.atan((g - mouseY) / 40.0F);
        Quaternionf quaternionf = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf quaternionf2 = new Quaternionf().rotateX(i * 20.0F * ((float)Math.PI / 180F));
        quaternionf.mul(quaternionf2);
        float j = entity.yBodyRot;
        float k = entity.getYRot();
        float l = entity.getXRot();
        float m = entity.yHeadRotO;
        float n = entity.yHeadRot;
        entity.yBodyRot = 180.0F + h * 20.0F;
        entity.setYRot(180.0F + h * 40.0F);
        entity.setXRot(-i * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        float o = entity.getScale();
        Vector3f vector3f = new Vector3f(0.0F, entity.getBbHeight() / 2.0F + yOffset * o, 0.0F);
        float p = (float)scale / o;
        renderEntity(guiGraphics, f, g, p, vector3f, quaternionf, quaternionf2, entity);
        entity.yBodyRot = j;
        entity.setYRot(k);
        entity.setXRot(l);
        entity.yHeadRotO = m;
        entity.yHeadRot = n;
        guiGraphics.disableScissor();
    }

    public static void renderEntity(GuiGraphics guiGraphics, float x, float y, float scale, Vector3f translate, Quaternionf pose, Quaternionf cameraOrientation, LivingEntity entity) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, (double)50.0F);
        poseStack.scale(scale, scale, -scale);
        poseStack.translate(translate.x, translate.y, translate.z);
        poseStack.mulPose(pose);
        Lighting.setupForEntityInInventory();
        var dispatcher = minecraft.getEntityRenderDispatcher();
        if (cameraOrientation != null) {
            dispatcher.overrideCameraOrientation(cameraOrientation.conjugate().rotateY((float)Math.PI));
        }
        dispatcher.setRenderShadow(false);
        dispatcher.render(entity, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, poseStack, guiGraphics.bufferSource(), 15728880);
        guiGraphics.flush();
        dispatcher.setRenderShadow(true);
        poseStack.popPose();
        Lighting.setupFor3DItems();
    }
}
