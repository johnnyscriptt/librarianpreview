package io.github.johnnyscriptt.librarianpreview.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.johnnyscriptt.librarianpreview.client.Highlights;
import io.github.johnnyscriptt.librarianpreview.client.LibrarianPreviewClient;
import io.github.johnnyscriptt.librarianpreview.client.TradeStore;
import io.github.johnnyscriptt.librarianpreview.client.config.ConfigIo;
import io.github.johnnyscriptt.librarianpreview.client.ui.SettingsScreen;
import io.github.johnnyscriptt.librarianpreview.net.OfferSnapshot;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Real in-game captures for the store pages. Not a pass/fail test: it exists so every store
 * image is an honest screenshot of the mod. Everything goes through the mod's own keys and
 * screens. Shots are taken at 960x540, where the GUI is laid out, and enlarged 2x by
 * {@code caption_gallery.py}.
 *
 * <p>Output: {@code build/run/clientGameTest/screenshots/store-*.png}.
 */
public final class StoreShotsGameTest implements FabricClientGameTest {
    /** The hall station whose librarian has a master's worth of trades. */
    private static final int LONG_LIST = 5;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(960, 540);
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            for (String command : new String[] {"time set noon", "weather clear",
                    "gamerule advance_time false", "difficulty peaceful", "gamemode creative @a"}) {
                world.getServer().runCommand(command);
            }

            BlockPos o = world.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                return player.blockPosition();
            });
            VillagerHall.build(world.getServer(), o);
            VillagerHall.staff(world.getServer(), o, LONG_LIST);
            List<BlockPos> lecterns = VillagerHall.lecterns(o);
            context.waitFor(client -> lecterns.stream().allMatch(l -> TradeStore.get(l) != null), 200);
            context.runOnClient(client -> {
                ConfigIo.get().highlights.clear();
                ConfigIo.get().rangeBlocks = 32;
            });

            // 01: the money shot, the book everyone hunts for, written in green.
            BlockPos centre = lecterns.get(VillagerHall.HALF);
            boolean found = world.getServer().computeOnServer(server -> {
                ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                Villager villager = level.getEntitiesOfClass(Villager.class, new AABB(centre.south()).inflate(0.5)).getFirst();
                for (int tries = 0; tries < 20000; tries++) {
                    if (sellsEfficiencyV(villager.getOffers())) {
                        return true;
                    }
                    villager.setOffers(null);
                }
                return false;
            });
            System.out.println("[store-shots] Efficiency V rolled: " + found);
            context.runOnClient(client -> ConfigIo.get().highlights.add("minecraft:efficiency@max"));
            context.waitFor(client -> TradeStore.get(centre) != null
                    && TradeStore.get(centre).stream().anyMatch(offer -> Highlights.matches(offer.result())), 100);
            standAt(context, world, centre.offset(0, 0, -1));
            lookAt(context, Vec3.atCenterOf(centre).add(0, 0.5, 0));
            settle(context, world);
            worldShot(context, "store-01-efficiency");
            context.runOnClient(client -> ConfigIo.get().highlights.clear());

            // 02: down the row from the walkway, the nearest sheets readable.
            standAt(context, world, lecterns.get(1).offset(-1, 0, -2));
            lookAt(context, Vec3.atCenterOf(lecterns.get(2)).add(0, 0.3, 0));
            settle(context, world);
            worldShot(context, "store-02-hall");

            // 03: picking the hunted books in the settings, with the real key and clicks.
            String hunted = huntThroughSettings(context, offersAt(context, lecterns.get(2)));
            worldShot(context, "store-03-settings");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitFor(client -> client.gui.screen() == null, 20);
            // A second hunted book further down the hall, added the same way a player would.
            String second = bookName(offersAt(context, lecterns.get(4)));
            context.runOnClient(client -> {
                if (second != null) {
                    ConfigIo.get().highlights.add(second);
                }
            });

            // 04: the hunted book, written in green.
            standAt(context, world, lecterns.get(2).offset(0, 0, -1));
            lookAt(context, Vec3.atCenterOf(lecterns.get(2)).add(0, 0.5, 0));
            settle(context, world);
            worldShot(context, "store-04-row");

            // 05: a long list, scrolled with the wheel while aiming at the lectern.
            BlockPos master = lecterns.get(LONG_LIST);
            standAt(context, world, master.offset(0, 0, -1));
            lookAt(context, Vec3.atCenterOf(master).add(0, 0.5, 0));
            settle(context, world);
            context.getInput().scroll(-3);
            context.waitTicks(5);
            worldShot(context, "store-05-scroll");

            context.runOnClient(client -> System.out.println("[store-shots] hunted " + hunted + ", " + second));
        }
    }

    private static boolean sellsEfficiencyV(List<MerchantOffer> offers) {
        for (MerchantOffer offer : offers) {
            ItemEnchantments stored = offer.getResult().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Holder<Enchantment> e : stored.keySet()) {
                if (e.is(Enchantments.EFFICIENCY) && stored.getLevel(e) == 5) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<OfferSnapshot> offersAt(ClientGameTestContext context, BlockPos lectern) {
        return context.computeOnClient(client -> TradeStore.get(lectern));
    }

    private static String bookName(List<OfferSnapshot> offers) {
        return offers.stream()
                .map(o -> o.result().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))
                .filter(e -> !e.isEmpty())
                .map(e -> e.keySet().iterator().next().getRegisteredName())
                .findFirst().orElse(null);
    }

    /** Opens settings with U, searches for the book's enchantment and clicks it. */
    private static String huntThroughSettings(ClientGameTestContext context, List<OfferSnapshot> offers) {
        String id = bookName(offers);
        String name = context.computeOnClient(client -> offers.stream()
                .map(o -> o.result().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))
                .filter(e -> !e.isEmpty())
                .findFirst().orElseThrow().keySet().iterator().next().value().description().getString());
        context.getInput().pressKey(LibrarianPreviewClient.SETTINGS_KEY);
        context.waitFor(client -> client.gui.screen() instanceof SettingsScreen, 20);
        context.getInput().typeChars(name.substring(0, Math.min(4, name.length())));
        context.waitTicks(3);
        double[] cursor = context.computeOnClient(client -> {
            SettingsScreen screen = (SettingsScreen) client.gui.screen();
            double scale = client.getWindow().getGuiScale();
            return new double[] {screen.rowCentreX() * scale, screen.rowCentreY(0) * scale};
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(8);
        return id;
    }

    /** Aims at a point rather than a block centre (the sheet sits on the lectern top). */
    private static void lookAt(ClientGameTestContext context, Vec3 target) {
        Vec3 eye = context.computeOnClient(client -> client.player.getEyePosition());
        Vec3 d = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        context.getInput().lookAt(yaw, pitch);
    }

    private static void worldShot(ClientGameTestContext context, String name) {
        context.runOnClient(client -> {
            client.gui.toastManager().clear();
            client.gui.hud.getChat().clearMessages(false);
        });
        context.waitTick();
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }

    /** Teleport, then wait for the client to agree, or lookAt aims at the sky. */
    private static void standAt(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos) {
        world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                .teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        context.waitFor(client -> client.player != null && client.player.blockPosition().equals(pos), 100);
    }

    /** Long enough for chunks to draw and the held item's name above the hotbar to fade. */
    private static void settle(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getConnection().waitForChunksRender();
        context.waitTicks(60);
    }
}
