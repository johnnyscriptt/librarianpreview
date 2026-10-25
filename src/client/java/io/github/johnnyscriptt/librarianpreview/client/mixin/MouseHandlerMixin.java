package io.github.johnnyscriptt.librarianpreview.client.mixin;

import io.github.johnnyscriptt.librarianpreview.client.LecternScroll;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The mouse wheel scrolls a lectern's trade sheet while you aim at it, instead of the hotbar. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void librarianpreview$scrollSheet(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (LecternScroll.onScroll(yOffset)) {
            ci.cancel();
        }
    }
}
