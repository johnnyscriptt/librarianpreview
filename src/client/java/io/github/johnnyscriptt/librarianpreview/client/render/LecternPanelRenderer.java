package io.github.johnnyscriptt.librarianpreview.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.johnnyscriptt.librarianpreview.client.TradeStore;
import io.github.johnnyscriptt.librarianpreview.client.config.ConfigIo;
import io.github.johnnyscriptt.librarianpreview.core.PanelLayout;
import io.github.johnnyscriptt.librarianpreview.core.config.LibrarianPreviewConfig;
import io.github.johnnyscriptt.librarianpreview.net.OfferSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draws a librarian's trades flat on the slanted top of its lectern, laid out like the rows
 * of the vanilla trade list. Called from the lectern block-entity renderer, so the pose is
 * already at the block's corner.
 */
public final class LecternPanelRenderer {
    /** Lectern top plate tilt from the block model (rotation -22.5 about x, origin 8,8,8). */
    private static final float PLATE_TILT = -22.5f;
    /** Centre of the plate along z in model space: the plate runs from z 3/16 to 16/16. */
    private static final float PLATE_CENTRE_Z = (3f / 16f + 1f) / 2f;
    private static final float TARGET_WIDTH = 1.3f;
    private static final float TARGET_HEIGHT = 0.8f;
    private static final float SURFACE_GAP = 0.004f;
    /** The book model sits in the middle of the plate; lift the panel clear of it. */
    private static final float BOOK_LIFT = 0.3f;
    private static final int MAX_LABEL_WIDTH = 120;

    // Vanilla container palette.
    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT_EDGE = 0xFFFFFFFF;
    private static final int DARK_EDGE = 0xFF555555;
    private static final int OUTLINE = 0xFF000000;
    private static final int TEXT = 0xFF404040;
    private static final int SOLD_OUT_TEXT = 0xFF8B8B8B;
    private static final int ARROW = 0xFF555555;
    private static final int SOLD_OUT_CROSS = 0xFFCC2222;

    private LecternPanelRenderer() {
    }

    public static void submit(LecternRenderState state, PoseStack pose, SubmitNodeCollector collector,
                              CameraRenderState camera) {
        LibrarianPreviewConfig config = ConfigIo.get();
        if (!config.enabled || state.blockPos == null) {
            return;
        }
        List<OfferSnapshot> offers = TradeStore.get(state.blockPos);
        if (offers == null || offers.isEmpty()) {
            return;
        }
        if (camera.pos != null && camera.pos.distanceToSqr(Vec3.atCenterOf(state.blockPos))
                > (double) config.rangeBlocks * config.rangeBlocks) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Component[] labels = new Component[offers.size()];
        int[] widths = new int[offers.size()];
        for (int i = 0; i < offers.size(); i++) {
            labels[i] = OfferLabels.label(offers.get(i).result());
            widths[i] = Math.min(MAX_LABEL_WIDTH, font.width(labels[i]));
        }
        PanelLayout layout = PanelLayout.of(widths);
        float scale = layout.scaleToFit(TARGET_WIDTH, TARGET_HEIGHT);

        pose.pushPose();
        // Block model rotation for this facing (the model faces north at 0).
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.rotateDegrees(Axis.YP, -(state.yRot + 90f));
        // Onto the tilted plate.
        pose.rotateDegrees(Axis.XP, PLATE_TILT);
        pose.translate(0f, 0.5f + SURFACE_GAP + (state.hasBook ? BOOK_LIFT : 0f), PLATE_CENTRE_Z - 0.5f);
        // Panel frame: x to the reader's right, y down the slope towards the reader, z up out
        // of the plate (the same left-handed GUI frame sign text uses).
        pose.rotateDegrees(Axis.XP, -90f);
        pose.scale(-scale, scale, scale);
        pose.translate(-layout.width() / 2f, -layout.height() / 2f, 0f);

        int light = LightCoordsUtil.FULL_BRIGHT;
        submitBackground(pose, collector, layout, offers);
        for (int row = 0; row < layout.rows(); row++) {
            OfferSnapshot offer = offers.get(row);
            int y = layout.rowY(row) + 1;
            submitStack(pose, collector, font, offer.costA(), PanelLayout.COST_A_X, y, light);
            if (!offer.costB().isEmpty()) {
                submitStack(pose, collector, font, offer.costB(), PanelLayout.COST_B_X, y, light);
            }
            submitStack(pose, collector, font, offer.result(), PanelLayout.RESULT_X, y, light);
            FormattedCharSequence text = clip(font, labels[row]);
            collector.submitText(pose, PanelLayout.LABEL_X, y + 4, text, false, Font.DisplayMode.POLYGON_OFFSET,
                    light, offer.outOfStock() ? SOLD_OUT_TEXT : TEXT, 0, 0);
        }
        pose.popPose();
    }

    private static FormattedCharSequence clip(Font font, Component label) {
        if (font.width(label) <= MAX_LABEL_WIDTH) {
            return label.getVisualOrderText();
        }
        String cut = font.plainSubstrByWidth(label.getString(), MAX_LABEL_WIDTH - font.width("..."));
        return Component.literal(cut + "...").withStyle(label.getStyle()).getVisualOrderText();
    }

    private static void submitStack(PoseStack pose, SubmitNodeCollector collector, Font font, ItemStack stack,
                                    int x, int y, int light) {
        if (stack.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ItemStackRenderState item = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(item, stack, ItemDisplayContext.GUI, mc.level, null, 0);
        pose.pushPose();
        pose.translate(x + 8f, y + 8f, 0.6f);
        // GUI item models are y-up; flip back to a right-handed frame and flatten the depth.
        pose.scale(16f, -16f, 1f);
        item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        if (stack.getCount() > 1) {
            String count = Integer.toString(stack.getCount());
            pose.pushPose();
            pose.translate(0f, 0f, 1.4f);
            collector.submitText(pose, x + 17 - font.width(count), y + 9, Component.literal(count).getVisualOrderText(),
                    true, Font.DisplayMode.POLYGON_OFFSET, light, 0xFFFFFFFF, 0, 0);
            pose.popPose();
        }
    }

    private static void submitBackground(PoseStack pose, SubmitNodeCollector collector, PanelLayout layout,
                                         List<OfferSnapshot> offers) {
        RenderType type = ShaderCompat.shaderPackInUse() ? RenderTypes.lightning() : RenderTypes.debugQuads();
        int w = layout.width();
        int h = layout.height();
        collector.submitCustomGeometry(pose, type, (p, vc) -> {
            // Vanilla bevel as strips that never overlap, so nothing z-fights on the plate.
            rect(p, vc, -1, -1, w + 1, 0, OUTLINE, 0f);
            rect(p, vc, -1, h, w + 1, h + 1, OUTLINE, 0f);
            rect(p, vc, -1, 0, 0, h, OUTLINE, 0f);
            rect(p, vc, w, 0, w + 1, h, OUTLINE, 0f);
            rect(p, vc, 0, 0, w - 1, 1, LIGHT_EDGE, 0f);
            rect(p, vc, 0, 1, 1, h - 1, LIGHT_EDGE, 0f);
            rect(p, vc, 1, h - 1, w, h, DARK_EDGE, 0f);
            rect(p, vc, w - 1, 1, w, h - 1, DARK_EDGE, 0f);
            rect(p, vc, w - 1, 0, w, 1, PANEL, 0f);
            rect(p, vc, 0, h - 1, 1, h, PANEL, 0f);
            rect(p, vc, 1, 1, w - 1, h - 1, PANEL, 0f);
        });
        // Translucent quads are depth-sorted, so arrows in the same batch can sort under the
        // panel; a later order draws them after it.
        collector.order(1).submitCustomGeometry(pose, type, (p, vc) -> {
            for (int row = 0; row < layout.rows(); row++) {
                int ay = layout.rowY(row) + 1 + 8;
                int ax = PanelLayout.ARROW_X;
                // A small pixel arrow like the trade screen's.
                rect(p, vc, ax, ay - 1, ax + 6, ay + 1, ARROW, 0.3f);
                rect(p, vc, ax + 6, ay - 3, ax + 7, ay + 3, ARROW, 0.3f);
                rect(p, vc, ax + 7, ay - 2, ax + 8, ay + 2, ARROW, 0.3f);
                rect(p, vc, ax + 8, ay - 1, ax + 9, ay + 1, ARROW, 0.3f);
                if (offers.get(row).outOfStock()) {
                    rect(p, vc, ax - 1, ay - 4, ax + 10, ay - 3, SOLD_OUT_CROSS, 0.3f);
                    rect(p, vc, ax - 1, ay + 3, ax + 10, ay + 4, SOLD_OUT_CROSS, 0.3f);
                }
            }
        });
    }

    /** One axis-aligned rectangle at height z above the plate, emitted with both windings. */
    private static void rect(PoseStack.Pose pose, VertexConsumer vc, float x0, float y0, float x1, float y1,
                             int argb, float z) {
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        vc.addVertex(pose, x0, y0, z).setColor(r, g, b, a);
        vc.addVertex(pose, x0, y1, z).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y1, z).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y0, z).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y0, z).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y1, z).setColor(r, g, b, a);
        vc.addVertex(pose, x0, y1, z).setColor(r, g, b, a);
        vc.addVertex(pose, x0, y0, z).setColor(r, g, b, a);
    }
}
