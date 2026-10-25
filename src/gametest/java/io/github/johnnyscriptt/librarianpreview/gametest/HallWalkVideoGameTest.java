package io.github.johnnyscriptt.librarianpreview.gametest;

import io.github.johnnyscriptt.librarianpreview.client.TradeStore;
import io.github.johnnyscriptt.librarianpreview.client.config.ConfigIo;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

/**
 * Frames for the store video: a slow sideways walk along the trading hall with every
 * lectern's sheet in view. One screenshot per frame, the HUD hidden (F1); ffmpeg turns
 * {@code walk-NNNN.png} into the clip. Only runs with {@code -Dlibrarianpreview.video=true}.
 */
public final class HallWalkVideoGameTest implements FabricClientGameTest {
    private static final int FPS = 30;
    /** Looking down onto the sheets from the walkway. */
    private static final float PITCH = 20;
    private static final double SECONDS = 14;
    private static final double EDGE = VillagerHall.HALF * VillagerHall.SPACING + 1.5;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("librarianpreview.video")) {
            return;
        }
        context.getInput().resizeWindow(1920, 1080);
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
            VillagerHall.staff(world.getServer(), o, -1);
            List<BlockPos> lecterns = VillagerHall.lecterns(o);
            context.waitFor(client -> lecterns.stream().allMatch(l -> TradeStore.get(l) != null), 200);
            context.runOnClient(client -> {
                ConfigIo.get().highlights.clear();
                ConfigIo.get().rangeBlocks = 32;
            });
            // Two hunted books along the way, so green writing passes by.
            for (int station : new int[] {2, 5}) {
                String id = context.computeOnClient(client -> TradeStore.get(lecterns.get(station)).stream()
                        .map(offer -> offer.result().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))
                        .filter(e -> !e.isEmpty())
                        .map(e -> e.keySet().iterator().next().getRegisteredName())
                        .findFirst().orElse(null));
                if (id != null) {
                    context.runOnClient(client -> ConfigIo.get().highlights.add(id));
                }
            }

            double y = o.getY();
            double z = o.getZ() + 0.6;
            place(context, o.getX() - EDGE, y, z);
            world.getServer().runCommand("tp @a " + (o.getX() - EDGE) + " " + y + " " + z + " 0 " + PITCH);
            context.runOnClient(client -> client.player.getAbilities().flying = true);
            context.getInput().pressKey(options -> options.keyToggleGui);
            world.getConnection().waitForChunksRender();
            context.waitTicks(60);

            int frames = (int) (FPS * SECONDS);
            for (int f = 0; f < frames; f++) {
                double t = (double) f / (frames - 1);
                // Ease in and out so the walk starts and stops gently.
                double eased = t * t * (3 - 2 * t);
                place(context, o.getX() - EDGE + eased * 2 * EDGE, y, z);
                context.waitTick();
                context.takeScreenshot(TestScreenshotOptions.of(String.format("walk-%04d", f)).disableCounterPrefix());
            }
            context.getInput().pressKey(options -> options.keyToggleGui);
        }
    }

    private static void place(ClientGameTestContext context, double x, double y, double z) {
        context.runOnClient(client -> {
            client.player.snapTo(x, y, z, 0, PITCH);
            client.player.setOldPosAndRot();
            client.player.setDeltaMovement(0, 0, 0);
        });
    }
}
