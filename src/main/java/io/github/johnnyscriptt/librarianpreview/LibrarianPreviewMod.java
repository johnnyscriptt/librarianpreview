package io.github.johnnyscriptt.librarianpreview;

import io.github.johnnyscriptt.librarianpreview.net.LecternTradesPayload;
import io.github.johnnyscriptt.librarianpreview.server.LecternScanner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. Villager trades only exist on the server, so the server half reads them
 * and sends them to clients that have the mod; in singleplayer this runs on the integrated
 * server.
 */
public final class LibrarianPreviewMod implements ModInitializer {
    public static final String MOD_ID = "librarianpreview";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(LecternTradesPayload.TYPE, LecternTradesPayload.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(LecternScanner::onEndTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LecternScanner.forget(handler.getPlayer().getUUID()));
    }
}
