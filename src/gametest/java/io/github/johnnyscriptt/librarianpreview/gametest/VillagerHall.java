package io.github.johnnyscriptt.librarianpreview.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A trading hall for the store shots and the walk-past video: a row of librarians, each
 * standing behind its own lectern, against a wall of bookshelves with lit spruce pillars.
 * The lecterns face north, so the walkway runs along x on the north side.
 */
final class VillagerHall {
    /** Distance between stations along x. */
    static final int SPACING = 4;
    /** Stations run from {@code -HALF} to {@code +HALF} (times SPACING) around the origin. */
    static final int HALF = 3;
    static final int LECTERN_Z = 2;

    private VillagerHall() {
    }

    static List<BlockPos> lecterns(BlockPos origin) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = -HALF; i <= HALF; i++) {
            out.add(origin.offset(i * SPACING, 0, LECTERN_Z));
        }
        return out;
    }

    static void build(TestServerContext server, BlockPos o) {
        int minX = o.getX() - HALF * SPACING - 4;
        int maxX = o.getX() + HALF * SPACING + 4;
        int y = o.getY();
        int z = o.getZ();
        server.runCommand(String.format("fill %d %d %d %d %d %d air", minX, y, z - 8, maxX, y + 8, z + 7));
        server.runCommand(String.format("fill %d %d %d %d %d %d dark_oak_planks", minX, y - 1, z - 8, maxX, y - 1, z + 6));
        server.runCommand(String.format("fill %d %d %d %d %d %d spruce_planks", minX, y - 1, z, maxX, y - 1, z + 1));
        server.runCommand(String.format("fill %d %d %d %d %d %d red_carpet", minX, y, z - 2, maxX, y, z - 2));
        server.runCommand(String.format("fill %d %d %d %d %d %d bookshelf", minX, y, z + 4, maxX, y + 3, z + 4));
        server.runCommand(String.format("fill %d %d %d %d %d %d spruce_slab[type=bottom]", minX, y + 4, z + 4, maxX, y + 4, z + 4));
        // End walls, so the hall reads as a room rather than a row in a field.
        for (int x : new int[] {minX, maxX}) {
            server.runCommand(String.format("fill %d %d %d %d %d %d bookshelf", x, y, z - 8, x, y + 3, z + 4));
            server.runCommand(String.format("fill %d %d %d %d %d %d spruce_log", x, y, z - 8, x, y + 4, z - 8));
        }
        server.runOnServer(s -> {
            ServerLevel level = s.getPlayerList().getPlayers().getFirst().level();
            BlockState log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
            BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false);
            for (int i = -HALF; i <= HALF + 1; i++) {
                int px = o.getX() + i * SPACING - SPACING / 2;
                for (int dy = 0; dy <= 3; dy++) {
                    level.setBlockAndUpdate(new BlockPos(px, y + dy, z + 4), Blocks.SPRUCE_LOG.defaultBlockState());
                }
                for (int dy = 0; dy <= 1; dy++) {
                    level.setBlockAndUpdate(new BlockPos(px, y + dy, z + 3), log);
                }
                level.setBlockAndUpdate(new BlockPos(px, y + 2, z + 3), lantern);
            }
            for (BlockPos lectern : lecterns(o)) {
                level.setBlockAndUpdate(lectern, Blocks.LECTERN.defaultBlockState()
                        .setValue(LecternBlock.FACING, Direction.NORTH));
            }
        });
    }

    /**
     * One still librarian behind every lectern, each re-rolled until it sells a book nobody
     * else in the hall sells, so the sheets differ. Station {@code longList} gets a master's
     * worth of trades so its sheet scrolls.
     */
    static void staff(TestServerContext server, BlockPos o, int longList) {
        Set<String> used = new HashSet<>();
        List<BlockPos> lecterns = lecterns(o);
        for (int i = 0; i < lecterns.size(); i++) {
            BlockPos lectern = lecterns.get(i);
            BlockPos at = lectern.south();
            server.runCommand("summon villager " + at.getX() + " " + at.getY() + " " + at.getZ()
                    + " {NoAI:1b,Rotation:[180f,0f],VillagerData:{profession:\"minecraft:librarian\",level:1,type:\"minecraft:plains\"}}");
            boolean master = i == longList;
            server.runOnServer(s -> {
                ServerLevel level = s.getPlayerList().getPlayers().getFirst().level();
                Villager villager = level.getEntitiesOfClass(Villager.class, new AABB(at).inflate(0.5)).getFirst();
                villager.setYHeadRot(180);
                villager.setYBodyRot(180);
                villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), lectern));
                for (int tries = 0; tries < 300; tries++) {
                    String book = bookOf(villager.getOffers());
                    if (book != null && used.add(book)) {
                        break;
                    }
                    villager.setOffers(null);
                }
                if (master) {
                    MerchantOffers all = new MerchantOffers();
                    for (int k = 0; k < 5; k++) {
                        for (MerchantOffer offer : villager.getOffers()) {
                            all.add(offer.copy());
                        }
                        villager.setOffers(null);
                    }
                    villager.setOffers(all);
                }
            });
        }
    }

    /** The registered name of the first enchanted book in {@code offers}, or null. */
    static String bookOf(List<MerchantOffer> offers) {
        for (MerchantOffer offer : offers) {
            ItemEnchantments stored = offer.getResult().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Holder<Enchantment> e : stored.keySet()) {
                return e.getRegisteredName();
            }
        }
        return null;
    }
}
