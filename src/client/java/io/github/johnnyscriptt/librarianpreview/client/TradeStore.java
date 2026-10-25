package io.github.johnnyscriptt.librarianpreview.client;

import io.github.johnnyscriptt.librarianpreview.net.LecternTradesPayload;
import io.github.johnnyscriptt.librarianpreview.net.OfferSnapshot;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Trades per claimed lectern, as last sent by the server. Client thread only. */
public final class TradeStore {
    private static final Map<BlockPos, List<OfferSnapshot>> TRADES = new HashMap<>();
    private static boolean serverHasMod;

    private TradeStore() {
    }

    public static void apply(LecternTradesPayload payload) {
        serverHasMod = true;
        if (payload.reset()) {
            TRADES.clear();
        }
        for (BlockPos pos : payload.removed()) {
            TRADES.remove(pos);
        }
        for (LecternTradesPayload.Entry entry : payload.entries()) {
            TRADES.put(entry.lectern(), List.copyOf(entry.offers()));
        }
    }

    public static List<OfferSnapshot> get(BlockPos lectern) {
        return TRADES.get(lectern);
    }

    public static int size() {
        return TRADES.size();
    }

    /** True once this connection has had any packet from the server half. */
    public static boolean serverHasMod() {
        return serverHasMod;
    }

    public static void clear() {
        TRADES.clear();
        serverHasMod = false;
    }
}
