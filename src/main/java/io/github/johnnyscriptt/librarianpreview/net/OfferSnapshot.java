package io.github.johnnyscriptt.librarianpreview.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/** One trade as the lectern panel draws it. {@code costB} is empty when the trade has no second cost. */
public record OfferSnapshot(ItemStack costA, ItemStack costB, ItemStack result, boolean outOfStock) {

    public static final StreamCodec<RegistryFriendlyByteBuf, OfferSnapshot> CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC, OfferSnapshot::costA,
                    ItemStack.OPTIONAL_STREAM_CODEC, OfferSnapshot::costB,
                    ItemStack.OPTIONAL_STREAM_CODEC, OfferSnapshot::result,
                    ByteBufCodecs.BOOL, OfferSnapshot::outOfStock,
                    OfferSnapshot::new);

    /** Base prices: reputation and Hero of the Village discounts are per player and only apply in the trade screen. */
    public static OfferSnapshot of(MerchantOffer offer) {
        return new OfferSnapshot(
                offer.getBaseCostA().copy(),
                offer.getCostB().copy(),
                offer.getResult().copy(),
                offer.isOutOfStock());
    }

    public int contentHash() {
        int h = hash(costA);
        h = 31 * h + hash(costB);
        h = 31 * h + hash(result);
        return 31 * h + (outOfStock ? 1 : 0);
    }

    private static int hash(ItemStack stack) {
        return 31 * ItemStack.hashItemAndComponents(stack) + stack.getCount();
    }
}
