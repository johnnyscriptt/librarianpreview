package io.github.johnnyscriptt.librarianpreview.core;

import java.util.List;

/**
 * Which enchantments the player is hunting for. Stored as plain strings in the config:
 * {@code "minecraft:mending"} for any level, {@code "minecraft:unbreaking@max"} for the top
 * level only. Edits write straight through to the list it wraps.
 */
public final class HighlightRules {
    private static final String MAX_SUFFIX = "@max";

    public enum Mode { OFF, ANY, MAX }

    private final List<String> entries;

    public HighlightRules(List<String> entries) {
        this.entries = entries;
    }

    public Mode mode(String enchantmentId) {
        if (entries.contains(enchantmentId + MAX_SUFFIX)) {
            return Mode.MAX;
        }
        return entries.contains(enchantmentId) ? Mode.ANY : Mode.OFF;
    }

    public boolean matches(String enchantmentId, int level, int maxLevel) {
        return switch (mode(enchantmentId)) {
            case OFF -> false;
            case ANY -> true;
            case MAX -> level >= maxLevel;
        };
    }

    /** Off, any level, max level only, off again. Max-only is skipped when there is one level. */
    public Mode cycle(String enchantmentId, int maxLevel) {
        Mode next = switch (mode(enchantmentId)) {
            case OFF -> Mode.ANY;
            case ANY -> maxLevel > 1 ? Mode.MAX : Mode.OFF;
            case MAX -> Mode.OFF;
        };
        entries.remove(enchantmentId);
        entries.remove(enchantmentId + MAX_SUFFIX);
        if (next == Mode.ANY) {
            entries.add(enchantmentId);
        } else if (next == Mode.MAX) {
            entries.add(enchantmentId + MAX_SUFFIX);
        }
        return next;
    }
}
