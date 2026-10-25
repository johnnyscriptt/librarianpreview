package io.github.johnnyscriptt.librarianpreview.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers the hash last sent for each key so only new, changed and vanished entries go
 * over the wire. One tracker per player.
 */
public final class SnapshotTracker<K> {
    private final Map<K, Integer> sent = new HashMap<>();

    public record Delta<K>(List<K> changed, List<K> removed) {
        public boolean isEmpty() {
            return changed.isEmpty() && removed.isEmpty();
        }
    }

    public Delta<K> update(Map<K, Integer> current) {
        List<K> changed = new ArrayList<>();
        for (Map.Entry<K, Integer> e : current.entrySet()) {
            Integer previous = sent.put(e.getKey(), e.getValue());
            if (previous == null || !previous.equals(e.getValue())) {
                changed.add(e.getKey());
            }
        }
        List<K> removed = new ArrayList<>();
        sent.keySet().removeIf(k -> {
            if (current.containsKey(k)) {
                return false;
            }
            removed.add(k);
            return true;
        });
        return new Delta<>(changed, removed);
    }
}
