package io.github.johnnyscriptt.librarianpreview.core;

/**
 * Geometry of one lectern panel in GUI pixels, laid out like a row of the vanilla trade
 * list: cost A, cost B, arrow, result, label.
 */
public record PanelLayout(int rows, int width, int height) {
    public static final int MAX_ROWS = 6;
    public static final int PADDING = 4;
    public static final int ROW_HEIGHT = 18;
    public static final int COST_A_X = PADDING;
    public static final int COST_B_X = COST_A_X + 20;
    public static final int ARROW_X = COST_B_X + 20;
    public static final int RESULT_X = ARROW_X + 12;
    public static final int LABEL_X = RESULT_X + 20;

    public static PanelLayout of(int[] labelWidths) {
        int rows = Math.min(MAX_ROWS, labelWidths.length);
        int widest = 0;
        for (int i = 0; i < rows; i++) {
            widest = Math.max(widest, labelWidths[i]);
        }
        return new PanelLayout(rows, LABEL_X + widest + PADDING, PADDING * 2 + rows * ROW_HEIGHT);
    }

    public int rowY(int row) {
        return PADDING + row * ROW_HEIGHT;
    }

    /** Uniform scale that fits the panel inside a target of the given size (in blocks). */
    public float scaleToFit(float targetWidth, float targetHeight) {
        return Math.min(targetWidth / width, targetHeight / height);
    }
}
