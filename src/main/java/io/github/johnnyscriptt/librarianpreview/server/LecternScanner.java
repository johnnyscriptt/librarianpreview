package io.github.johnnyscriptt.librarianpreview.server;

import io.github.johnnyscriptt.librarianpreview.core.SnapshotTracker;
import io.github.johnnyscriptt.librarianpreview.net.LecternTradesPayload;
import io.github.johnnyscriptt.librarianpreview.net.OfferSnapshot;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Once a second, finds librarians near each player that have claimed a lectern and sends the
 * trades of any lectern whose trades changed. Only reads loaded blocks; never loads chunks.
 */
public final class LecternScanner {
    public static final int INTERVAL_TICKS = 20;
    /** Covers the largest client draw range (32) plus a villager standing off its lectern. */
    private static final double SCAN_RANGE = 40;
    private static final int MAX_LECTERNS = 32;

    private record PlayerState(ResourceKey<Level> dimension, SnapshotTracker<BlockPos> tracker) {
    }

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();
    private static int tick;

    private LecternScanner() {
    }

    public static void onEndTick(MinecraftServer server) {
        if (++tick % INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, LecternTradesPayload.TYPE)) {
                scan(player);
            }
        }
    }

    public static void forget(UUID player) {
        STATES.remove(player);
    }

    private static void scan(ServerPlayer player) {
        ServerLevel level = player.level();
        PlayerState state = STATES.get(player.getUUID());
        boolean reset = false;
        if (state == null || state.dimension() != level.dimension()) {
            state = new PlayerState(level.dimension(), new SnapshotTracker<>());
            STATES.put(player.getUUID(), state);
            reset = true;
        }

        Map<BlockPos, List<OfferSnapshot>> found = findLecterns(level, player);
        Map<BlockPos, Integer> hashes = new HashMap<>();
        found.forEach((pos, offers) -> {
            int h = 1;
            for (OfferSnapshot offer : offers) {
                h = 31 * h + offer.contentHash();
            }
            hashes.put(pos, h);
        });

        SnapshotTracker.Delta<BlockPos> delta = state.tracker().update(hashes);
        if (delta.isEmpty() && !reset) {
            return;
        }
        List<LecternTradesPayload.Entry> entries = new ArrayList<>();
        for (BlockPos pos : delta.changed()) {
            entries.add(new LecternTradesPayload.Entry(pos, found.get(pos)));
        }
        ServerPlayNetworking.send(player, new LecternTradesPayload(reset, entries, delta.removed()));
    }

    private static Map<BlockPos, List<OfferSnapshot>> findLecterns(ServerLevel level, ServerPlayer player) {
        Map<BlockPos, List<OfferSnapshot>> found = new LinkedHashMap<>();
        AABB box = player.getBoundingBox().inflate(SCAN_RANGE);
        for (Villager villager : level.getEntitiesOfClass(Villager.class, box, Villager::isAlive)) {
            if (found.size() >= MAX_LECTERNS) {
                break;
            }
            if (!villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
                continue;
            }
            Optional<GlobalPos> site = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            if (site.isEmpty() || site.get().dimension() != level.dimension()) {
                continue;
            }
            BlockPos pos = site.get().pos();
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(Blocks.LECTERN)) {
                continue;
            }
            List<OfferSnapshot> offers = new ArrayList<>();
            // Rolls the level-1 trades if nobody has opened the screen yet, exactly as the
            // first right-click would. It does not lock the profession (that needs XP).
            for (MerchantOffer offer : villager.getOffers()) {
                offers.add(OfferSnapshot.of(offer));
            }
            found.put(pos.immutable(), offers);
        }
        return found;
    }
}
