package io.github.johnnyscriptt.librarianpreview;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint: payload registration and server-side handlers go here, so the mod works
 * on dedicated servers and, through the integrated server, in singleplayer.
 */
public final class LibrarianPreviewMod implements ModInitializer {
    public static final String MOD_ID = "librarianpreview";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
    }
}
