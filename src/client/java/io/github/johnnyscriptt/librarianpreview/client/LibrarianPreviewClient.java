package io.github.johnnyscriptt.librarianpreview.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import static io.github.johnnyscriptt.librarianpreview.LibrarianPreviewMod.MOD_ID;

/** Client entrypoint: keys, HUD, screens and client networking. */
public final class LibrarianPreviewClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    // Pick a key nobody in the user's instance owns (references/scaffold.md, Default keys).
    public static final KeyMapping OPEN_KEY =
            new KeyMapping("key." + MOD_ID + ".open", InputConstants.UNKNOWN.getValue(), CATEGORY);

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(OPEN_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.level == null || mc.gui.screen() != null) {
                return;
            }
            while (OPEN_KEY.consumeClick()) {
                // open the mod's screen
            }
        });
    }
}
