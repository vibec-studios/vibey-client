package dev.henny.vibeyclient.module.inventory.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchQueryTest {
    private final ItemFacts pick = ItemFacts.of("Diamond Pickaxe", "minecraft:diamond_pickaxe", List.of("Efficiency V"));
    private final ItemFacts box = ItemFacts.of("Shulker Box", "minecraft:shulker_box", List.of("Diamond", "Torch"));
    private final ItemFacts modded = ItemFacts.of("Copper Wire", "techmod:copper_wire", List.of());
    private final ItemFacts german = ItemFacts.of("Größerer Würfel", "minecraft:stone", List.of());

    @Test
    void emptyQueryMatchesNothingSpecial() {
        assertTrue(SearchQuery.parse("  ").isEmpty());
        assertTrue(SearchQuery.parse(null).isEmpty());
    }

    @Test
    void wordsAreAndedAndCaseFree() {
        assertTrue(SearchQuery.parse("diamond PICK").matches(pick));
        assertFalse(SearchQuery.parse("diamond sword").matches(pick));
    }

    @Test
    void idsEnchantsAndContentsCount() {
        assertTrue(SearchQuery.parse("diamond_pickaxe").matches(pick));
        assertTrue(SearchQuery.parse("efficiency").matches(pick));
        assertTrue(SearchQuery.parse("torch").matches(box));
    }

    @Test
    void modFilterAndNegation() {
        assertTrue(SearchQuery.parse("@tech").matches(modded));
        assertFalse(SearchQuery.parse("@tech").matches(pick));
        assertTrue(SearchQuery.parse("diamond -box").matches(pick));
        assertFalse(SearchQuery.parse("diamond -box").matches(box));
    }

    @Test
    void accentsAndSharpSFold() {
        assertTrue(SearchQuery.parse("grosserer wurfel").matches(german));
        assertTrue(SearchQuery.parse("größ").matches(german));
    }
}
