package io.github.johnnyscriptt.librarianpreview.core.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibrarianPreviewConfigTest {

    @Test
    void defaultsAreEnabledAtTwelveBlocks() {
        LibrarianPreviewConfig config = new LibrarianPreviewConfig();
        assertTrue(config.enabled);
        assertEquals(12, config.rangeBlocks);
    }

    @Test
    void clampKeepsRangeBetweenFourAndThirtyTwo() {
        LibrarianPreviewConfig config = new LibrarianPreviewConfig();
        config.rangeBlocks = 1;
        config.clamp();
        assertEquals(4, config.rangeBlocks);
        config.rangeBlocks = 500;
        config.clamp();
        assertEquals(32, config.rangeBlocks);
    }

    @Test
    void clampTreatsZeroAsAbsentAndTakesDefault() {
        LibrarianPreviewConfig config = new LibrarianPreviewConfig();
        config.rangeBlocks = 0;
        config.clamp();
        assertEquals(12, config.rangeBlocks);
    }
}
