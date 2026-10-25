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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static io.github.johnnyscriptt.librarianpreview.LibrarianPreviewMod.MOD_ID;

/**
 * Draws a librarian's trades in white handwriting on a sheet of old dark paper lying on its lectern,
 * laid out like the rows of the vanilla trade list. Called from the lectern block-entity
 * renderer, so the pose is already at the block's corner.
 */
public final class LecternPanelRenderer {
    private static final Identifier PAPER = Identifier.fromNamespaceAndPath(MOD_ID, "textures/paper.png");
    private static final FontDescription PENCIL =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath(MOD_ID, "pencil"));

    /** Lectern top plate tilt from the block model (rotation -22.5 about x, origin 8,8,8). */
    private static final float PLATE_TILT = -22.5f;
    /** Centre of the plate along z in model space: the plate runs from z 3/16 to 16/16. */
    private static final float PLATE_CENTRE_Z = (3f / 16f + 1f) / 2f;
    /** Just enough to beat z-fighting with the plate; the sheet must look like it lies on it. */
    private static final float SURFACE_GAP = 0.0015f;
    /** Sheet size in blocks, the texture's 30x24 aspect. */
    private static final float PAPER_W = 0.94f;
    private static final float PAPER_H = 0.752f;
    /** Writing area inside the yellowed rim (2 of 30 and 24 texture pixels per side). */
    private static final float WRITE_W = PAPER_W * 26f / 30f;
    private static final float WRITE_H = PAPER_H * 20f / 24f;
    /** One text size on every lectern: a full-width row fills the writing area's width. */
    private static final float SCALE = WRITE_W / PanelLayout.WIDTH;
    private static final int VISIBLE_ROWS = PanelLayout.visibleRows(WRITE_H / SCALE);

    private static final int INK = 0xFFFFFFFF;
    private static final int SOLD_OUT = 0xFF8A8278;
    /** Paper-coloured wash over the item icons so they look sketched onto the sheet. */
    private static final int WASH = 0x70FFFFFF;

    private LecternPanelRenderer() {
    }

    /** Trade rows that fit on a sheet before it scrolls. */
    public static int visibleRows() {
        return VISIBLE_ROWS;
    }

    public static void submit(LecternRenderState state, PoseStack pose, SubmitNodeCollector collector,
                              CameraRenderState camera) {
        LibrarianPreviewConfig config = ConfigIo.get();
        // An open book would cover the sheet, and hunting lecterns are normally empty.
        if (!config.enabled || state.blockPos == null || state.hasBook) {
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

        Font font = Minecraft.getInstance().font;
        int total = offers.size();
        int first = PanelLayout.clampScroll(TradeStore.scroll(state.blockPos), total, VISIBLE_ROWS);
        int shown = Math.min(VISIBLE_ROWS, total - first);
        int light = state.lightCoords;

        pose.pushPose();
        // Block model rotation for this facing (the model faces north at 0).
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.rotateDegrees(Axis.YP, -(state.yRot + 90f));
        // Onto the tilted plate.
        pose.rotateDegrees(Axis.XP, PLATE_TILT);
        pose.translate(0f, 0.5f + SURFACE_GAP, PLATE_CENTRE_Z - 0.5f);
        // Sheet frame: x to the reader's right, y down the slope towards the reader, z up out
        // of the plate (the same left-handed GUI frame sign text uses).
        pose.rotateDegrees(Axis.XP, -90f);
        pose.scale(-1f, 1f, 1f);
        // Dropped by hand, not squared up: a small fixed twist per lectern.
        pose.rotateDegrees(Axis.ZP, twist(state));

        // The text pipeline: lit by the lightmap only, like a sign, so the sheet keeps its
        // colour from every angle (entity shading greyed it out at a distance).
        collector.submitCustomGeometry(pose, RenderTypes.text(PAPER), (p, vc) ->
                quad(p, vc, -PAPER_W / 2f, -PAPER_H / 2f, PAPER_W / 2f, PAPER_H / 2f, 0f,
                        0f, 0f, 1f, 1f, 0xFFFFFFFF, light));

        // Writing starts at the top-left of the writing area (the far, high end of the plate).
        pose.translate(-WRITE_W / 2f, -WRITE_H / 2f, 0f);
        pose.scale(SCALE, SCALE, SCALE);
        for (int i = 0; i < shown; i++) {
            OfferSnapshot offer = offers.get(first + i);
            submitRow(pose, collector, font, offer, wrapLabel(font, offer), PanelLayout.rowY(i) + 1, light);
        }
        // Scroll marks in the right-hand rim when rows are hidden above or below.
        int markX = PanelLayout.WIDTH + 2;
        if (first > 0) {
            text(pose, collector, Component.literal("↑").getVisualOrderText(), markX,
                    PanelLayout.rowY(0) + 1, light, INK);
        }
        if (first + shown < total) {
            text(pose, collector, Component.literal("↓").getVisualOrderText(), markX,
                    PanelLayout.rowY(shown - 1) + 10, light, INK);
        }
        pose.popPose();
    }

    /** Paper texture coordinates under a point of the writing, for washes that match the sheet. */
    private static float u(float x) {
        return (x * SCALE + (PAPER_W - WRITE_W) / 2f) / PAPER_W;
    }

    private static float v(float y) {
        return (y * SCALE + (PAPER_H - WRITE_H) / 2f) / PAPER_H;
    }

    private static void submitRow(PoseStack pose, SubmitNodeCollector collector, Font font, OfferSnapshot offer,
                                  List<FormattedCharSequence> label, int y, int light) {
        int colour = offer.outOfStock() ? SOLD_OUT : INK;
        submitStack(pose, collector, font, offer.costA(), PanelLayout.COST_A_X, y, light, colour);
        if (!offer.costB().isEmpty()) {
            submitStack(pose, collector, font, offer.costB(), PanelLayout.COST_B_X, y, light, colour);
        }
        text(pose, collector, pencil(Component.literal("→")).getVisualOrderText(),
                PanelLayout.ARROW_X, y + 4, light, colour);
        submitStack(pose, collector, font, offer.result(), PanelLayout.RESULT_X, y, light, colour);
        int lineY = label.size() == 1 ? y + 4 : y;
        for (FormattedCharSequence line : label) {
            text(pose, collector, line, PanelLayout.LABEL_X, lineY, light, colour);
            lineY += font.lineHeight;
        }
    }

    /** A label in pencil, wrapped to at most two lines so long enchant names stay large. */
    private static List<FormattedCharSequence> wrapLabel(Font font, OfferSnapshot offer) {
        MutableComponent label = pencil(OfferLabels.label(offer.result()));
        if (offer.outOfStock()) {
            label.withStyle(s -> s.withStrikethrough(true));
        }
        List<FormattedCharSequence> split = font.split(label, PanelLayout.LINE_WIDTH);
        return split.size() > 2 ? split.subList(0, 2) : split;
    }

    private static MutableComponent pencil(Component text) {
        return Component.empty().append(text).withStyle(s -> s.withFont(PENCIL));
    }

    private static void text(PoseStack pose, SubmitNodeCollector collector, FormattedCharSequence text,
                             float x, float y, int light, int colour) {
        collector.submitText(pose, x, y, text, false, Font.DisplayMode.POLYGON_OFFSET, light, colour, 0, 0);
    }

    private static void submitStack(PoseStack pose, SubmitNodeCollector collector, Font font, ItemStack stack,
                                    int x, int y, int light, int colour) {
        if (stack.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ItemStackRenderState item = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(item, stack, ItemDisplayContext.GUI, mc.level, null, 0);
        pose.pushPose();
        pose.translate(x + 8f, y + 8f, 0.25f);
        // GUI item models are y-up; flip back to a right-handed frame and press them flat.
        pose.scale(16f, -16f, 0.4f);
        item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        // Fade the icon into the paper: a see-through copy of the exact patch of sheet under it.
        // The sheet is twisted as a whole, so the patch still lines up.
        collector.order(1).submitCustomGeometry(pose, RenderTypes.text(PAPER), (p, vc) ->
                quad(p, vc, x, y, x + 16, y + 16, 0.6f,
                        u(x), v(y), u(x + 16), v(y + 16), WASH, light));

        if (stack.getCount() > 1) {
            String count = Integer.toString(stack.getCount());
            FormattedCharSequence seq = pencil(Component.literal(count)).getVisualOrderText();
            pose.pushPose();
            pose.translate(0f, 0f, 0.8f);
            text(pose, collector, seq, x + 17 - font.width(seq), y + 9, light, colour);
            pose.popPose();
        }
    }

    private static float twist(LecternRenderState state) {
        return (Math.floorMod(state.blockPos.hashCode(), 7) - 3) * 0.8f;
    }

    /** A textured rectangle at height z, emitted with both windings. */
    private static void quad(PoseStack.Pose pose, VertexConsumer vc, float x0, float y0, float x1, float y1,
                             float z, float u0, float v0, float u1, float v1, int argb, int light) {
        vertex(pose, vc, x0, y0, z, u0, v0, argb, light);
        vertex(pose, vc, x0, y1, z, u0, v1, argb, light);
        vertex(pose, vc, x1, y1, z, u1, v1, argb, light);
        vertex(pose, vc, x1, y0, z, u1, v0, argb, light);
        vertex(pose, vc, x1, y0, z, u1, v0, argb, light);
        vertex(pose, vc, x1, y1, z, u1, v1, argb, light);
        vertex(pose, vc, x0, y1, z, u0, v1, argb, light);
        vertex(pose, vc, x0, y0, z, u0, v0, argb, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z,
                               float u, float v, int argb, int light) {
        vc.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 0f, 1f);
    }
}
