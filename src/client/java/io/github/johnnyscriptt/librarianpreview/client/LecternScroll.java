package io.github.johnnyscriptt.librarianpreview.client;

import io.github.johnnyscriptt.librarianpreview.client.config.ConfigIo;
import io.github.johnnyscriptt.librarianpreview.client.render.LecternPanelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Turns a mouse-wheel step into a one-row scroll of the sheet on the lectern under the
 * crosshair. Only claims the wheel when that sheet has more trades than fit, so everywhere
 * else the hotbar scrolls as normal.
 */
public final class LecternScroll {
    /** Wheel travel not yet turned into a whole row (touchpads send small fractions). */
    private static double pending;
    private static BlockPos pendingFor;

    private LecternScroll() {
    }

    /** @return true when the scroll was used by a sheet and must not reach the hotbar */
    public static boolean onScroll(double yOffset) {
        Minecraft mc = Minecraft.getInstance();
        if (yOffset == 0 || mc.player == null || mc.level == null || mc.gui.screen() != null
                || mc.player.isSpectator() || !ConfigIo.get().enabled) {
            return false;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        var state = mc.level.getBlockState(pos);
        // The sheet is hidden under a book, so there is nothing to scroll then.
        if (!state.is(Blocks.LECTERN) || state.getValue(LecternBlock.HAS_BOOK)) {
            return false;
        }
        if (!pos.equals(pendingFor)) {
            pendingFor = pos.immutable();
            pending = 0;
        }
        pending += yOffset * mc.options.mouseWheelSensitivity().get();
        int notches = (int) pending;
        pending -= notches;
        // Wheel up shows earlier trades, like scrolling a page.
        return TradeStore.scrollBy(pos, -notches, LecternPanelRenderer.visibleRows());
    }
}
