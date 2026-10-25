package io.github.johnnyscriptt.librarianpreview.core;

/**
 * Geometry of the writing on a lectern sheet in GUI pixels, laid out like a row of the
 * vanilla trade list: cost A, cost B, arrow, result, label. The text keeps one size; when
 * the trades do not fit, the sheet shows a window of whole rows that scrolls.
 */
public final class PanelLayout {
    public static final int PADDING = 4;
    public static final int ROW_HEIGHT = 18;
    /** Labels wrap at this width, onto at most two lines. */
    public static final int LINE_WIDTH = 72;
    public static final int COST_A_X = PADDING;
    public static final int COST_B_X = COST_A_X + 20;
    public static final int ARROW_X = COST_B_X + 20;
    public static final int RESULT_X = ARROW_X + 12;
    public static final int LABEL_X = RESULT_X + 20;
    public static final int WIDTH = LABEL_X + LINE_WIDTH + PADDING;

    private PanelLayout() {
    }

    public static int rowY(int row) {
        return PADDING + row * ROW_HEIGHT;
    }

    /** Whole rows that fit in a writing area this many pixels tall. */
    public static int visibleRows(float heightPx) {
        return Math.max(1, (int) ((heightPx - PADDING * 2) / ROW_HEIGHT));
    }

    public static boolean scrollable(int totalRows, int visibleRows) {
        return totalRows > visibleRows;
    }

    /** First row to show: never above the top, never past the point where the last row is in view. */
    public static int clampScroll(int offset, int totalRows, int visibleRows) {
        int max = Math.max(0, totalRows - visibleRows);
        return Math.max(0, Math.min(offset, max));
    }
}
