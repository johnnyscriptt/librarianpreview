package io.github.johnnyscriptt.librarianpreview.core.config;

/** Client settings, stored as config/librarianpreview.json. */
public final class LibrarianPreviewConfig {
    public static final int DEFAULT_RANGE = 12;
    public static final int MIN_RANGE = 4;
    public static final int MAX_RANGE = 32;

    public boolean enabled = true;
    /** Lecterns further than this from the player are not drawn. */
    public int rangeBlocks = DEFAULT_RANGE;

    public void clamp() {
        // 0 means "written by a version without the field": take the default.
        if (rangeBlocks == 0) {
            rangeBlocks = DEFAULT_RANGE;
        }
        rangeBlocks = Math.max(MIN_RANGE, Math.min(MAX_RANGE, rangeBlocks));
    }
}
