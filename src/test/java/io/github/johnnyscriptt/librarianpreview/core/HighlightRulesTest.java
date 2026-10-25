package io.github.johnnyscriptt.librarianpreview.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighlightRulesTest {

    @Test
    void nothingIsHighlightedByDefault() {
        HighlightRules rules = new HighlightRules(new ArrayList<>());
        assertFalse(rules.matches("minecraft:mending", 1, 1));
        assertEquals(HighlightRules.Mode.OFF, rules.mode("minecraft:mending"));
    }

    @Test
    void anyLevelMatchesEveryLevel() {
        HighlightRules rules = new HighlightRules(new ArrayList<>(List.of("minecraft:unbreaking")));
        assertTrue(rules.matches("minecraft:unbreaking", 1, 3));
        assertTrue(rules.matches("minecraft:unbreaking", 3, 3));
        assertFalse(rules.matches("minecraft:sharpness", 5, 5));
    }

    @Test
    void maxOnlyMatchesTheTopLevel() {
        HighlightRules rules = new HighlightRules(new ArrayList<>(List.of("minecraft:unbreaking@max")));
        assertFalse(rules.matches("minecraft:unbreaking", 2, 3));
        assertTrue(rules.matches("minecraft:unbreaking", 3, 3));
        assertEquals(HighlightRules.Mode.MAX, rules.mode("minecraft:unbreaking"));
    }

    @Test
    void cycleGoesOffAnyMaxOff() {
        List<String> stored = new ArrayList<>();
        HighlightRules rules = new HighlightRules(stored);
        assertEquals(HighlightRules.Mode.ANY, rules.cycle("minecraft:unbreaking", 3));
        assertEquals(List.of("minecraft:unbreaking"), stored);
        assertEquals(HighlightRules.Mode.MAX, rules.cycle("minecraft:unbreaking", 3));
        assertEquals(List.of("minecraft:unbreaking@max"), stored);
        assertEquals(HighlightRules.Mode.OFF, rules.cycle("minecraft:unbreaking", 3));
        assertTrue(stored.isEmpty());
    }

    @Test
    void singleLevelEnchantmentsSkipMaxOnly() {
        HighlightRules rules = new HighlightRules(new ArrayList<>());
        assertEquals(HighlightRules.Mode.ANY, rules.cycle("minecraft:mending", 1));
        assertEquals(HighlightRules.Mode.OFF, rules.cycle("minecraft:mending", 1));
    }
}
