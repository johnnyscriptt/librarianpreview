package io.github.johnnyscriptt.librarianpreview.gametest;

import io.github.johnnyscriptt.librarianpreview.client.TradeStore;
import io.github.johnnyscriptt.librarianpreview.net.OfferSnapshot;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;

import java.util.List;

/**
 * The multiplayer path: a real dedicated server and a client joining it over the network.
 * Trades must reach the client, and clear when the lectern goes.
 */
public final class DedicatedServerGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(960, 540);
        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            connection.waitForChunksRender();
            server.runCommand("time set noon");
            server.runCommand("difficulty peaceful");

            BlockPos base = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().blockPosition());
            BlockPos lectern = base.offset(0, 0, 2);
            server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().level().setBlockAndUpdate(lectern,
                    Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH)));
            LecternTradesGameTest.spawnLibrarian(server, lectern, base.offset(0, 0, 4));

            context.waitFor(client -> TradeStore.get(lectern) != null, 200);
            List<OfferSnapshot> offers = context.computeOnClient(client -> TradeStore.get(lectern));
            if (offers.stream().noneMatch(o -> o.result().has(DataComponents.STORED_ENCHANTMENTS))) {
                throw new AssertionError("enchanted book did not survive the network codec: " + offers);
            }
            context.getInput().lookAt(lectern);
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("dedicated-server").disableCounterPrefix());

            server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().level()
                    .setBlockAndUpdate(lectern, Blocks.AIR.defaultBlockState()));
            context.waitFor(client -> TradeStore.get(lectern) == null, 60);
        }
    }
}
