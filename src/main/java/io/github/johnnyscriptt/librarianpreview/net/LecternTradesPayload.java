package io.github.johnnyscriptt.librarianpreview.net;

import io.github.johnnyscriptt.librarianpreview.LibrarianPreviewMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server to client: trades for lecterns that are new or changed since the last packet, and
 * lecterns that no longer have a librarian (or left range). {@code reset} clears everything
 * first; it is set on the first packet and after a dimension change.
 */
public record LecternTradesPayload(boolean reset, List<Entry> entries, List<BlockPos> removed)
        implements CustomPacketPayload {

    public record Entry(BlockPos lectern, List<OfferSnapshot> offers) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, Entry::lectern,
                        OfferSnapshot.CODEC.apply(ByteBufCodecs.list(16)), Entry::offers,
                        Entry::new);
    }

    public static final Type<LecternTradesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LibrarianPreviewMod.MOD_ID, "lectern_trades"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LecternTradesPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, LecternTradesPayload::reset,
                    Entry.CODEC.apply(ByteBufCodecs.list(64)), LecternTradesPayload::entries,
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(256)), LecternTradesPayload::removed,
                    LecternTradesPayload::new);

    @Override
    public Type<LecternTradesPayload> type() {
        return TYPE;
    }
}
