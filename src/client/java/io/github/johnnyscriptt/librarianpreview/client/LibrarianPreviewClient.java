package io.github.johnnyscriptt.librarianpreview.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.johnnyscriptt.librarianpreview.client.config.ConfigIo;
import io.github.johnnyscriptt.librarianpreview.core.config.LibrarianPreviewConfig;
import io.github.johnnyscriptt.librarianpreview.net.LecternTradesPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static io.github.johnnyscriptt.librarianpreview.LibrarianPreviewMod.MOD_ID;

/** Client entrypoint: receives lectern trades from the server and the show/hide key. */
public final class LibrarianPreviewClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    public static final KeyMapping TOGGLE_KEY =
            new KeyMapping("key." + MOD_ID + ".toggle", InputConstants.KEY_H, CATEGORY);

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(TOGGLE_KEY);
        ClientPlayNetworking.registerGlobalReceiver(LecternTradesPayload.TYPE,
                (payload, context) -> TradeStore.apply(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> TradeStore.clear());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.level == null || mc.gui.screen() != null) {
                return;
            }
            while (TOGGLE_KEY.consumeClick()) {
                toggle(mc);
            }
        });
    }

    private static void toggle(Minecraft mc) {
        LibrarianPreviewConfig config = ConfigIo.get();
        config.enabled = !config.enabled;
        ConfigIo.save();
        String key;
        if (config.enabled && !TradeStore.serverHasMod()) {
            key = "message." + MOD_ID + ".server_missing";
        } else {
            key = config.enabled ? "message." + MOD_ID + ".shown" : "message." + MOD_ID + ".hidden";
        }
        mc.player.sendOverlayMessage(Component.translatable(key));
    }
}
