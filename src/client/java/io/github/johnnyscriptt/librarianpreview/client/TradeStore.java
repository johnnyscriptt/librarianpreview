package io.github.johnnyscriptt.librarianpreview.client;

import io.github.johnnyscriptt.librarianpreview.core.PanelLayout;
import io.github.johnnyscriptt.librarianpreview.net.LecternTradesPayload;
import io.github.johnnyscriptt.librarianpreview.net.OfferSnapshot;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Trades per claimed lectern, as last sent by the server, and how far each sheet is scrolled. Client thread only. */
public final class TradeStore {
    private static final Map<BlockPos, List<OfferSnapshot>> TRADES = new HashMap<>();
    private static final Map<BlockPos, Integer> SCROLL = new HashMap<>();
    private static boolean serverHasMod;

    private TradeStore() {
    }

    public static void apply(LecternTradesPayload payload) {
        serverHasMod = true;
        if (payload.reset()) {
            TRADES.clear();
            SCROLL.clear();
        }
        for (BlockPos pos : payload.removed()) {
            TRADES.remove(pos);
            SCROLL.remove(pos);
        }
        for (LecternTradesPayload.Entry entry : payload.entries()) {
            TRADES.put(entry.lectern(), List.copyOf(entry.offers()));
        }
    }

    public static List<OfferSnapshot> get(BlockPos lectern) {
        return TRADES.get(lectern);
    }

    /** First trade row shown on this lectern's sheet. */
    public static int scroll(BlockPos lectern) {
        return SCROLL.getOrDefault(lectern, 0);
    }

    /**
     * Moves this lectern's sheet by {@code rows}. Returns false when the trades all fit, so
     * the caller can let the scroll through to the hotbar.
     */
    public static boolean scrollBy(BlockPos lectern, int rows, int visibleRows) {
        List<OfferSnapshot> offers = TRADES.get(lectern);
        if (offers == null || !PanelLayout.scrollable(offers.size(), visibleRows)) {
            return false;
        }
        SCROLL.put(lectern, PanelLayout.clampScroll(scroll(lectern) + rows, offers.size(), visibleRows));
        return true;
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
        SCROLL.clear();
        serverHasMod = false;
    }
}
