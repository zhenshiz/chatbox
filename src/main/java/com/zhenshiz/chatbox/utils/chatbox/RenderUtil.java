package com.zhenshiz.chatbox.utils.chatbox;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RenderUtil {

    public static void renderImage(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int x, int y, int z, int uw, int uh, int width, int height) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        bufferBuilder.addVertex(matrix4f, x, y, z).setUv(0, 0);
        bufferBuilder.addVertex(matrix4f, x, y + height, z).setUv(0, uh);
        bufferBuilder.addVertex(matrix4f, x + width, y + height, z).setUv(uw, uh);
        bufferBuilder.addVertex(matrix4f, x + width, y, z).setUv(uw, 0);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, resourceLocation);
        RenderSystem.enableBlend();
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    public static void renderImage(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int x, int y, int z, int width, int height) {
        renderImage(guiGraphics, resourceLocation, x, y, z, 1, 1, width, height);
    }

    public static void handleGameProfileAsync(String input, Consumer<GameProfile> postAction) {
        ResolvableProfile component = createProfileComponent(input);
        component.resolve()
                .thenApplyAsync(result -> {
                    GameProfile profile = result.gameProfile();
                    postAction.accept(profile);
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

    public static Supplier<PlayerSkin> texturesSupplier(GameProfile profile) {
        Minecraft minecraft = Minecraft.getInstance();
        SkinManager skinManager = minecraft.getSkinManager();
        CompletableFuture<PlayerSkin> completableFuture = skinManager.getOrLoad(profile);
        boolean bl = !minecraft.isLocalPlayer(profile.getId());
        PlayerSkin playerSkin = DefaultPlayerSkin.get(profile);
        return () -> {
            PlayerSkin PlayerSkin2 = completableFuture.getNow(playerSkin);
            return bl && !PlayerSkin2.secure() ? playerSkin : PlayerSkin2;
        };
    }
}
