package io.github.johnnyscriptt.librarianpreview.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelLayoutTest {

    @Test
    void widthFitsTheLabelColumnAfterTheResultSlot() {
        assertEquals(PanelLayout.LABEL_X + PanelLayout.LINE_WIDTH + PanelLayout.PADDING, PanelLayout.WIDTH);
    }

    @Test
    void rowIsCompactSoTheWritingDrawsLarge() {
        // The sheet is a fixed size, so a narrower row is drawn at a larger scale.
        assertTrue(PanelLayout.WIDTH <= 132, "row width " + PanelLayout.WIDTH);
    }

    @Test
    void iconsAndArrowDoNotOverlap() {
        assertTrue(PanelLayout.COST_B_X - PanelLayout.COST_A_X >= 16);
        assertTrue(PanelLayout.ARROW_X - PanelLayout.COST_B_X >= 16);
        assertTrue(PanelLayout.RESULT_X - PanelLayout.ARROW_X >= 9);
        assertTrue(PanelLayout.LABEL_X - PanelLayout.RESULT_X >= 17);
    }

    @Test
    void rowYStartsBelowTopPadding() {
        assertEquals(PanelLayout.PADDING, PanelLayout.rowY(0));
        assertEquals(PanelLayout.PADDING + 2 * PanelLayout.ROW_HEIGHT, PanelLayout.rowY(2));
    }

    @Test
    void visibleRowsCountsWholeRowsInsideThePadding() {
        int height = PanelLayout.PADDING * 2 + 3 * PanelLayout.ROW_HEIGHT + PanelLayout.ROW_HEIGHT - 1;
        assertEquals(3, PanelLayout.visibleRows(height));
    }

    @Test
    void visibleRowsIsAtLeastOne() {
        assertEquals(1, PanelLayout.visibleRows(2));
    }

    @Test
    void shortListsDoNotScroll() {
        assertFalse(PanelLayout.scrollable(4, 6));
        assertFalse(PanelLayout.scrollable(6, 6));
        assertTrue(PanelLayout.scrollable(7, 6));
    }

    @Test
    void scrollStopsAtTheTop() {
        assertEquals(0, PanelLayout.clampScroll(-3, 9, 6));
    }

    @Test
    void scrollStopsWhenTheLastRowIsInView() {
        assertEquals(3, PanelLayout.clampScroll(5, 9, 6));
        assertEquals(2, PanelLayout.clampScroll(2, 9, 6));
    }

    @Test
    void scrollIsZeroWhenEverythingFits() {
        assertEquals(0, PanelLayout.clampScroll(4, 3, 6));
    }
}
