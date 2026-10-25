package io.github.johnnyscriptt.librarianpreview.gametest;

import io.github.johnnyscriptt.librarianpreview.client.TradeStore;
import io.github.johnnyscriptt.librarianpreview.net.OfferSnapshot;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A librarian bound to a lectern: the server half must send its trades and the client must
 * hold them for that lectern, including an enchanted book with its stored enchantment.
 * Screenshots go to {@code build/run/clientGameTest/screenshots/} for a visual check.
 */
public final class LecternTradesGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(960, 540);
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            for (String command : new String[] {"time set noon", "weather clear",
                    "gamerule advance_time false", "difficulty peaceful"}) {
                world.getServer().runCommand(command);
            }

            BlockPos base = world.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                return player.blockPosition();
            });
            BlockPos plain = base.offset(0, 0, 2);
            BlockPos withBook = base.offset(-2, 0, 2);
            world.getServer().runOnServer(server -> {
                ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                level.setBlockAndUpdate(plain, Blocks.LECTERN.defaultBlockState()
                        .setValue(LecternBlock.FACING, Direction.NORTH));
                level.setBlockAndUpdate(withBook, Blocks.LECTERN.defaultBlockState()
                        .setValue(LecternBlock.FACING, Direction.NORTH).setValue(LecternBlock.HAS_BOOK, true));
            });
            spawnLibrarian(world.getServer(), plain, base.offset(0, 0, 4));
            spawnLibrarian(world.getServer(), withBook, base.offset(-2, 0, 4));

            context.waitFor(client -> TradeStore.get(plain) != null && TradeStore.get(withBook) != null, 200);
            List<OfferSnapshot> offers = context.computeOnClient(client -> TradeStore.get(plain));
            boolean hasBook = offers.stream().anyMatch(o ->
                    o.result().has(DataComponents.STORED_ENCHANTMENTS));
            if (!hasBook) {
                throw new AssertionError("expected an enchanted book trade with stored enchantments, got " + offers);
            }

            standAt(context, world, base);
            lookAt(context, Vec3.atCenterOf(plain).add(0, 0.55, 0));
            settle(context, world);
            shot(context, "lectern-close");

            lookAt(context, Vec3.atCenterOf(base.offset(-1, 0, 2)).add(0, 0.5, 0));
            settle(context, world);
            shot(context, "lectern-pair");

            BlockPos side = base.offset(3, 1, 3);
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                    .teleportTo(side.getX() + 0.5, base.getY(), side.getZ() + 0.5));
            context.waitTicks(20);
            lookAt(context, Vec3.atCenterOf(plain).add(0, 0.5, 0));
            settle(context, world);
            shot(context, "lectern-side");

            standAt(context, world, base.offset(0, 0, 1));
            lookAt(context, Vec3.atCenterOf(plain).add(0, 0.5, 0));
            settle(context, world);
            shot(context, "lectern-top");

            checkScrolling(context, world, base);

            // Breaking the lectern drops its panel on the next scan.
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().level()
                    .setBlockAndUpdate(plain, Blocks.AIR.defaultBlockState()));
            context.waitFor(client -> TradeStore.get(plain) == null, 60);
        }
    }

    /**
     * A lectern with more trades than fit: the writing starts at the top, and the mouse wheel
     * moves it one row at a time without touching the hotbar.
     */
    private static void checkScrolling(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base) {
        BlockPos lectern = base.offset(2, 0, 2);
        BlockPos at = base.offset(2, 0, 4);
        world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().getFirst().level().setBlockAndUpdate(
                lectern, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH)));
        spawnLibrarian(world.getServer(), lectern, at);
        // Five level-1 rolls on one villager: a list as long as a master librarian's.
        world.getServer().runOnServer(s -> {
            ServerLevel level = s.getPlayerList().getPlayers().getFirst().level();
            Villager villager = level.getEntitiesOfClass(Villager.class, new AABB(at).inflate(0.5)).getFirst();
            MerchantOffers all = new MerchantOffers();
            for (int i = 0; i < 5; i++) {
                for (MerchantOffer offer : villager.getOffers()) {
                    all.add(offer.copy());
                }
                villager.setOffers(null);
            }
            villager.setOffers(all);
        });
        context.waitFor(client -> TradeStore.get(lectern) != null && TradeStore.get(lectern).size() == 10, 100);

        standAt(context, world, base.offset(2, 0, 1));
        lookAt(context, Vec3.atCenterOf(lectern).add(0, 0.5, 0));
        settle(context, world);
        shot(context, "scroll-top");

        int slot = context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
        context.getInput().scroll(-2);
        context.waitTicks(5);
        int scrolled = context.computeOnClient(client -> TradeStore.scroll(lectern));
        int slotAfter = context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
        if (scrolled != 2 || slotAfter != slot) {
            throw new AssertionError("wheel should move the sheet 2 rows and leave the hotbar; scroll="
                    + scrolled + " slot " + slot + "->" + slotAfter);
        }
        shot(context, "scroll-middle");

        context.getInput().scroll(-20);
        context.waitTicks(5);
        shot(context, "scroll-end");
        context.getInput().scroll(20);
        context.waitTicks(5);
        if (context.computeOnClient(client -> TradeStore.scroll(lectern)) != 0) {
            throw new AssertionError("scrolling back up should stop at the first trade");
        }
    }

    /**
     * A still librarian already bound to {@code lectern}, re-rolled until it offers a book, so
     * the test does not wait on villager AI or luck.
     */
    static void spawnLibrarian(TestServerContext server, BlockPos lectern, BlockPos at) {
        server.runCommand("summon villager " + at.getX() + " " + at.getY() + " " + at.getZ()
                + " {NoAI:1b,VillagerData:{profession:\"minecraft:librarian\",level:1,type:\"minecraft:plains\"}}");
        server.runOnServer(s -> {
            ServerLevel level = s.getPlayerList().getPlayers().getFirst().level();
            Villager villager = level.getEntitiesOfClass(Villager.class, new AABB(at).inflate(0.5)).getFirst();
            villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), lectern));
            for (int i = 0; i < 100 && !offersBook(villager); i++) {
                villager.setOffers(null);
            }
        });
    }

    private static boolean offersBook(Villager villager) {
        for (MerchantOffer offer : villager.getOffers()) {
            if (offer.getResult().has(DataComponents.STORED_ENCHANTMENTS)) {
                return true;
            }
        }
        return false;
    }

    /** Aims at a point rather than a block centre (the panel sits on the lectern top). */
    private static void lookAt(ClientGameTestContext context, Vec3 target) {
        Vec3 eye = context.computeOnClient(client -> client.player.getEyePosition());
        Vec3 d = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        context.getInput().lookAt(yaw, pitch);
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.runOnClient(client -> {
            client.gui.toastManager().clear();
            client.gui.hud.getChat().clearMessages(false);
        });
        context.waitTick();
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }

    private static void standAt(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos) {
        world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                .teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        context.waitFor(client -> client.player != null && client.player.blockPosition().equals(pos), 100);
    }

    private static void settle(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getConnection().waitForChunksRender();
        context.waitTicks(30);
    }
}
