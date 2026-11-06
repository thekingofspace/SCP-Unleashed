package net.rai.scp_unleashed.scp244;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class Scp244Frost {
    public static final int FrostTicks = 10;
    public static final int Samples = 6;
    public static final float IceLevel = 0.45f;
    public static final float IceChance = 0.2f;
    public static final float SnowLevel = 0.6f;
    public static final float SnowChance = 0.05f;
    public static final int MaxLayers = 2;

    private Scp244Frost() {
    }

    public static void tick(ServerLevel World, Scp244VaseBlockEntity Vase, Scp244Field Field) {
        if (World.getGameTime() % FrostTicks != Math.floorMod(Vase.getBlockPos().hashCode(), FrostTicks) || Field.size() <= 1) {
            return;
        }
        if (!World.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        RandomSource Random = World.random;
        BlockPos Origin = Vase.getBlockPos();
        for (int Try = 0; Try < Samples; Try++) {
            int Slot = Random.nextInt(Field.size());
            float Density = Vase.density(Field.distance(Slot));
            if (Density < IceLevel) {
                continue;
            }
            BlockPos Cell = BlockPos.of(Field.key(Slot));
            if (Cell.equals(Origin)) {
                continue;
            }
            BlockPos Below = Cell.below();
            BlockState Under = World.getBlockState(Below);
            if (Under.is(Blocks.WATER) && Under.getFluidState().isSource()) {
                if (Random.nextFloat() < IceChance * Density) {
                    World.setBlockAndUpdate(Below, Blocks.ICE.defaultBlockState());
                    World.playSound(null, Below, Scp244Registry.Freeze.get(), SoundSource.BLOCKS, 0.6f, 0.9f + Random.nextFloat() * 0.2f);
                }
                continue;
            }
            if (Density < SnowLevel || Random.nextFloat() >= SnowChance * Density) {
                continue;
            }
            BlockState Here = World.getBlockState(Cell);
            if (Here.isAir()) {
                BlockState Snow = Blocks.SNOW.defaultBlockState();
                if (Snow.canSurvive(World, Cell)) {
                    World.setBlockAndUpdate(Cell, Snow);
                }
            } else if (Here.is(Blocks.SNOW) && Here.getValue(SnowLayerBlock.LAYERS) < MaxLayers) {
                World.setBlockAndUpdate(Cell, Here.setValue(SnowLayerBlock.LAYERS, Here.getValue(SnowLayerBlock.LAYERS) + 1));
            }
        }
    }
}
