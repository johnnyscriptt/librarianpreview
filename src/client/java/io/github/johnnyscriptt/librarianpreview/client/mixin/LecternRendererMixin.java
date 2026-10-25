package io.github.johnnyscriptt.librarianpreview.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.johnnyscriptt.librarianpreview.client.render.LecternPanelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the trade panel to every lectern; at HEAD because vanilla returns early without a book. */
@Mixin(LecternRenderer.class)
public abstract class LecternRendererMixin {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/LecternRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"))
    private void librarianpreview$submitPanel(LecternRenderState state, PoseStack pose,
                                              SubmitNodeCollector collector, CameraRenderState camera,
                                              CallbackInfo ci) {
        LecternPanelRenderer.submit(state, pose, collector, camera);
    }
}
