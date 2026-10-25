package io.github.johnnyscriptt.librarianpreview.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PanelLayoutTest {

    @Test
    void widthFitsTheWidestLabelAfterTheResultSlot() {
        PanelLayout layout = PanelLayout.of(new int[] {30, 52});
        assertEquals(PanelLayout.LABEL_X + 52 + PanelLayout.PADDING, layout.width());
    }

    @Test
    void heightStacksRowsWithPadding() {
        PanelLayout layout = PanelLayout.of(new int[] {10, 10, 10});
        assertEquals(PanelLayout.PADDING * 2 + 3 * PanelLayout.ROW_HEIGHT, layout.height());
    }

    @Test
    void rowsAreCappedAtMaxRows() {
        PanelLayout layout = PanelLayout.of(new int[PanelLayout.MAX_ROWS + 4]);
        assertEquals(PanelLayout.MAX_ROWS, layout.rows());
    }

    @Test
    void rowYStartsBelowTopPadding() {
        PanelLayout layout = PanelLayout.of(new int[] {10, 10});
        assertEquals(PanelLayout.PADDING, layout.rowY(0));
        assertEquals(PanelLayout.PADDING + PanelLayout.ROW_HEIGHT, layout.rowY(1));
    }

    @Test
    void scaleFitsTheTighterSideOfTheTarget() {
        PanelLayout layout = PanelLayout.of(new int[] {10});
        float scale = layout.scaleToFit(1.0f, 1.0f);
        float expected = Math.min(1.0f / layout.width(), 1.0f / layout.height());
        assertEquals(expected, scale, 1e-6);
    }

    @Test
    void emptyPanelStillHasAMinimumWidth() {
        PanelLayout layout = PanelLayout.of(new int[0]);
        assertEquals(PanelLayout.LABEL_X + PanelLayout.PADDING, layout.width());
        assertEquals(PanelLayout.PADDING * 2, layout.height());
    }
}
