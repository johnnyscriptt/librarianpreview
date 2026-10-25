package io.github.johnnyscriptt.librarianpreview.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotTrackerTest {

    @Test
    void firstUpdateReportsEverythingAsChanged() {
        SnapshotTracker<String> tracker = new SnapshotTracker<>();
        SnapshotTracker.Delta<String> delta = tracker.update(Map.of("a", 1, "b", 2));
        assertEquals(Set.of("a", "b"), Set.copyOf(delta.changed()));
        assertTrue(delta.removed().isEmpty());
    }

    @Test
    void unchangedHashesAreNotResent() {
        SnapshotTracker<String> tracker = new SnapshotTracker<>();
        tracker.update(Map.of("a", 1, "b", 2));
        SnapshotTracker.Delta<String> delta = tracker.update(Map.of("a", 1, "b", 3));
        assertEquals(List.of("b"), delta.changed());
        assertTrue(delta.removed().isEmpty());
    }

    @Test
    void missingKeysAreReportedRemovedOnce() {
        SnapshotTracker<String> tracker = new SnapshotTracker<>();
        tracker.update(Map.of("a", 1, "b", 2));
        SnapshotTracker.Delta<String> delta = tracker.update(Map.of("a", 1));
        assertEquals(List.of("b"), delta.removed());
        assertTrue(delta.changed().isEmpty());
        assertTrue(tracker.update(Map.of("a", 1)).isEmpty());
    }

    @Test
    void reappearingKeyIsChangedAgain() {
        SnapshotTracker<String> tracker = new SnapshotTracker<>();
        tracker.update(Map.of("a", 1));
        tracker.update(Map.of());
        assertEquals(List.of("a"), tracker.update(Map.of("a", 1)).changed());
    }
}
