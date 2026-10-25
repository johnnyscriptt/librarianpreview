package io.github.johnnyscriptt.librarianpreview.client;

import io.github.johnnyscriptt.librarianpreview.client.config.ConfigIo;
import io.github.johnnyscriptt.librarianpreview.core.HighlightRules;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** The player's hunted enchantments, as set in the settings screen. */
public final class Highlights {
    private Highlights() {
    }

    public static HighlightRules rules() {
        return new HighlightRules(ConfigIo.get().highlights);
    }

    /** True when the trade's result is a book carrying an enchantment the player is hunting. */
    public static boolean matches(ItemStack result) {
        if (ConfigIo.get().highlights.isEmpty()) {
            return false;
        }
        ItemEnchantments stored = result.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        HighlightRules rules = rules();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            if (rules.matches(enchantment.getRegisteredName(), entry.getIntValue(), enchantment.value().getMaxLevel())) {
                return true;
            }
        }
        return false;
    }
}
