package net.rai.scp_unleashed.daeviteruins.world;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.daeviteruins.plan.RuinMat;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public final class DaeviteRuinsPalette {
    private static final Logger Log = com.mojang.logging.LogUtils.getLogger();
    private static final Map<Integer, BlockState> Cache = new HashMap<>();

    private DaeviteRuinsPalette() {
    }

    public static Rotation turnFor(Direction Front) {
        return switch (Front) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos toWorld(BlockPos Origin, Direction Front, int X, int Y, int Z) {
        return Origin.offset(new BlockPos(X, Y, Z).rotate(turnFor(Front)));
    }

    public static BlockState state(RuinMat Mat, int Facing, int Variant) {
        int Key = Mat.ordinal() | (Facing & 7) << 8 | (Variant & 255) << 12;
        BlockState Found = Cache.get(Key);
        if (Found != null) {
            return Found;
        }
        String Text = Mat.state(Facing, Variant);
        BlockState Parsed;
        try {
            Parsed = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), Text, false).blockState();
        } catch (CommandSyntaxException Problem) {
            Log.warn("Daevite ruin palette could not parse {}: {}", Text, Problem.getMessage());
            Parsed = Blocks.STONE_BRICKS.defaultBlockState();
        }
        Cache.put(Key, Parsed);
        return Parsed;
    }

    public static BlockState placed(RuinMat Mat, int Facing, int Variant, Direction Front) {
        return state(Mat, Facing, Variant).rotate(turnFor(Front));
    }

    public static BlockState growth(ServerLevel World, BlockPos Pos, double Roll) {
        Holder<Biome> Here = World.getBiome(Pos);
        if (Here.is(Tags.Biomes.IS_SNOWY) || Here.is(BiomeTags.SPAWNS_SNOW_FOXES)) {
            return Blocks.SNOW.defaultBlockState();
        }
        if (Here.is(Tags.Biomes.IS_DESERT) || Here.is(BiomeTags.IS_BADLANDS) || Here.is(Tags.Biomes.IS_DRY)) {
            return Roll < 0.5 ? Blocks.DEAD_BUSH.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        if (Here.is(BiomeTags.IS_TAIGA) || Here.is(BiomeTags.IS_JUNGLE)) {
            return Roll < 0.45 ? Blocks.FERN.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState();
        }
        return Roll < 0.85 ? Blocks.SHORT_GRASS.defaultBlockState() : Blocks.FERN.defaultBlockState();
    }

    public static boolean natural(BlockState State) {
        if (State.isAir() || !State.getFluidState().isEmpty() && State.getBlock() instanceof LiquidBlock) {
            return true;
        }
        if (State.is(BlockTags.DIRT) || State.is(BlockTags.SAND) || State.is(BlockTags.BASE_STONE_OVERWORLD) || State.is(BlockTags.TERRACOTTA) || State.is(BlockTags.LOGS)
                || State.is(BlockTags.LEAVES) || State.is(BlockTags.FLOWERS) || State.is(BlockTags.SAPLINGS) || State.is(BlockTags.REPLACEABLE)
                || State.is(BlockTags.REPLACEABLE_BY_TREES) || State.is(BlockTags.ICE) || State.is(BlockTags.SNOW) || State.is(Tags.Blocks.ORES)
                || State.is(BlockTags.CAVE_VINES) || State.is(BlockTags.CORALS) || State.is(BlockTags.CORAL_BLOCKS)) {
            return true;
        }
        Block Kind = State.getBlock();
        return Kind == Blocks.GRAVEL || Kind == Blocks.CLAY || Kind == Blocks.SNOW_BLOCK || Kind == Blocks.POWDER_SNOW || Kind == Blocks.SANDSTONE || Kind == Blocks.RED_SANDSTONE
                || Kind == Blocks.CALCITE || Kind == Blocks.TUFF || Kind == Blocks.DRIPSTONE_BLOCK || Kind == Blocks.POINTED_DRIPSTONE || Kind == Blocks.MOSS_BLOCK
                || Kind == Blocks.MOSS_CARPET || Kind == Blocks.MUD || Kind == Blocks.MUDDY_MANGROVE_ROOTS || Kind == Blocks.MANGROVE_ROOTS || Kind == Blocks.BAMBOO
                || Kind == Blocks.BAMBOO_SAPLING || Kind == Blocks.SUGAR_CANE || Kind == Blocks.CACTUS || Kind == Blocks.KELP || Kind == Blocks.KELP_PLANT
                || Kind == Blocks.VINE || Kind == Blocks.GLOW_LICHEN || Kind == Blocks.SWEET_BERRY_BUSH || Kind == Blocks.PUMPKIN || Kind == Blocks.MELON
                || Kind == Blocks.BEE_NEST || Kind == Blocks.BROWN_MUSHROOM_BLOCK || Kind == Blocks.RED_MUSHROOM_BLOCK || Kind == Blocks.MUSHROOM_STEM
                || Kind == Blocks.BROWN_MUSHROOM || Kind == Blocks.RED_MUSHROOM || Kind == Blocks.OBSIDIAN || Kind == Blocks.MAGMA_BLOCK || Kind == Blocks.PACKED_ICE
                || Kind == Blocks.BLUE_ICE || Kind == Blocks.SMOOTH_BASALT || Kind == Blocks.AMETHYST_BLOCK || Kind == Blocks.BUDDING_AMETHYST || Kind == Blocks.COCOA
                || Kind == Blocks.SEAGRASS || Kind == Blocks.TALL_SEAGRASS || Kind == Blocks.SEA_PICKLE || Kind == Blocks.BIG_DRIPLEAF || Kind == Blocks.BIG_DRIPLEAF_STEM
                || Kind == Blocks.SMALL_DRIPLEAF || Kind == Blocks.HANGING_ROOTS || Kind == Blocks.SPORE_BLOSSOM || Kind == Blocks.AZALEA || Kind == Blocks.FLOWERING_AZALEA
                || Kind == Blocks.PINK_PETALS;
    }

    public static boolean growthLike(BlockState State) {
        return State.isAir() || State.is(BlockTags.LOGS) || State.is(BlockTags.LEAVES) || State.is(BlockTags.REPLACEABLE_BY_TREES) || State.is(BlockTags.REPLACEABLE)
                || State.is(Blocks.BAMBOO) || State.is(Blocks.BAMBOO_SAPLING) || State.is(Blocks.CACTUS) || State.is(Blocks.SUGAR_CANE) || State.is(Blocks.COCOA)
                || State.is(Blocks.MUSHROOM_STEM) || State.is(Blocks.RED_MUSHROOM_BLOCK) || State.is(Blocks.BROWN_MUSHROOM_BLOCK) || State.is(Blocks.SNOW)
                || State.is(Blocks.BEE_NEST) || State.is(Blocks.VINE) || State.is(BlockTags.FLOWERS) || State.is(BlockTags.SAPLINGS);
    }
}
