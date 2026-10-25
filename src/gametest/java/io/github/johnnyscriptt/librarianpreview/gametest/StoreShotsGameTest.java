package io.github.johnnyscriptt.librarianpreview.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Predicate;

/**
 * Real in-game captures for the store pages. Not a pass/fail test: it exists so every store
 * image is an honest screenshot of the mod. Everything goes through the mod's own keys and
 * screens. Shots are taken at 960x540, where the GUI is laid out, and enlarged 2x by
 * {@code caption_gallery.py}.
 *
 * <p>Output: {@code build/run/clientGameTest/screenshots/store-*.png}.
 */
public final class StoreShotsGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(960, 540);
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            for (String command : new String[] {"time set noon", "weather clear",
                    "gamerule advance_time false", "difficulty normal"}) {
                world.getServer().runCommand(command);
            }

            BlockPos base = world.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos origin = player.blockPosition();
                buildScene(player.level(), origin);
                return origin;
            });

            // 01: the money shot, the mod doing the thing someone installs it for.
            standAt(context, world, base);
            context.getInput().lookAt(base.offset(0, 1, 3));
            settle(context, world);
            // context.getInput().pressKey(LibrarianPreviewClient.OPEN_KEY);
            worldShot(context, "store-01-main");

            // 02..NN: one feature per shot, then the settings screen, then optionally the
            // mod's keybinds in vanilla Controls. Never a "nothing found" result.
        }
    }

    /** A small, tidy scene in front of the player. Keep each scene 40 blocks from the next. */
    private static void buildScene(ServerLevel level, BlockPos base) {
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 2; y++) {
                level.setBlockAndUpdate(base.offset(x, y, 5), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        level.setBlockAndUpdate(base.offset(0, 0, 3), Blocks.CHEST.defaultBlockState());
    }

    /** Screens: let the GUI settle first. */
    private static void shot(ClientGameTestContext context, String name) {
        context.waitTicks(8);
        worldShot(context, name);
    }

    /** In-world: taken straight away, while any action-bar message is still up. */
    private static void worldShot(ClientGameTestContext context, String name) {
        context.runOnClient(client -> {
            client.gui.toastManager().clear();
            client.gui.hud.getChat().clearMessages(false);
        });
        context.waitTick();
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }

    private static boolean waitUntil(ClientGameTestContext context, Predicate<Minecraft> done, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            if (context.computeOnClient(done::test)) {
                return true;
            }
            context.waitTick();
        }
        return false;
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
