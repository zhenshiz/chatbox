package com.zhenshiz.chatbox.mixin.client;

import com.zhenshiz.chatbox.editor.EditorPreviewPack;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

/** 仅向客户端资源包仓库添加会话预览资源，服务端数据包仓库保持原状。 */
@Mixin(PackRepository.class)
public abstract class EditorPackRepositoryMixin {
    @Shadow @Final @Mutable private Set<RepositorySource> sources;

    @Inject(method = "reload", at = @At("HEAD"))
    private void chatbox$addPreviewResources(CallbackInfo callback) {
        if ((Object) this == Minecraft.getInstance().getResourcePackRepository() && !sources.contains(EditorPreviewPack.SOURCE)) {
            sources = new HashSet<>(sources);
            sources.add(EditorPreviewPack.SOURCE);
        }
    }
}
