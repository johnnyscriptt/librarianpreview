package io.github.johnnyscriptt.librarianpreview.client.render;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** The text next to a trade's result: the enchantment for a book, the item name otherwise. */
public final class OfferLabels {
    private OfferLabels() {
    }

    public static Component label(ItemStack result) {
        ItemEnchantments stored = result.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stored.isEmpty()) {
            return Component.literal(result.getHoverName().getString());
        }
        MutableComponent label = Component.empty();
        boolean first = true;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            if (!first) {
                label.append(", ");
            }
            first = false;
            Holder<Enchantment> enchantment = entry.getKey();
            // getFullname styles normal enchantments grey; drop that so the label takes the
            // panel's ink colour, and keep only the curse red.
            MutableComponent name = Component.literal(
                    Enchantment.getFullname(enchantment, entry.getIntValue()).getString());
            if (enchantment.is(EnchantmentTags.CURSE)) {
                name.withStyle(ChatFormatting.RED);
            }
            label.append(name);
        }
        return label;
    }
}
