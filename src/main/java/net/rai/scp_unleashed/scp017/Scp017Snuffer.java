package net.rai.scp_unleashed.scp017;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

public final class Scp017Snuffer {
    public static final TagKey<Block> Snuffable = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp017_snuffable"));
    public static final TagKey<Block> Dousable = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp017_dousable"));
    public static final int Radius = 5;
    public static final int Interval = 20;
    public static final double CentreLift = 1.0;

    private Scp017Snuffer() {
    }

    public static boolean breaks(BlockState State) {
        return State.is(Snuffable) && (!State.hasProperty(BlockStateProperties.LIT) || State.getValue(BlockStateProperties.LIT));
    }

    public static boolean douses(BlockState State) {
        return State.is(Dousable) && State.hasProperty(BlockStateProperties.LIT) && State.getValue(BlockStateProperties.LIT);
    }

    public static boolean target(BlockState State, boolean Griefing) {
        return douses(State) || Griefing && breaks(State);
    }

    public static boolean griefing(ServerLevel World, Entity Shadow) {
        return EventHooks.canEntityGrief(World, Shadow);
    }

    @Nullable
    public static BlockPos nearest(ServerLevel World, Vec3 Centre, boolean Griefing) {
        BlockPos Middle = BlockPos.containing(Centre);
        double Limit = (Radius + 0.5) * (Radius + 0.5);
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Cell : BlockPos.betweenClosed(Middle.offset(-Radius, -Radius, -Radius), Middle.offset(Radius, Radius, Radius))) {
            double Distance = Cell.distToCenterSqr(Centre);
            if (Distance > Limit || Distance >= BestDistance || !World.isLoaded(Cell)) {
                continue;
            }
            if (target(World.getBlockState(Cell), Griefing)) {
                Best = Cell.immutable();
                BestDistance = Distance;
            }
        }
        return Best;
    }

    public static boolean anyNear(ServerLevel World, Entity Shadow, Vec3 Centre) {
        return nearest(World, Centre, griefing(World, Shadow)) != null;
    }

    public static boolean snuffNearest(ServerLevel World, Entity Shadow) {
        BlockPos Found = nearest(World, Shadow.position().add(0.0, CentreLift, 0.0), griefing(World, Shadow));
        if (Found == null) {
            return false;
        }
        snuff(World, Shadow, Found);
        return true;
    }

    public static void snuff(ServerLevel World, Entity Shadow, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (douses(State)) {
            if (State.getBlock() instanceof CampfireBlock) {
                CampfireBlock.dowse(Shadow, World, Pos, State);
            }
            World.setBlock(Pos, State.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL_IMMEDIATE);
        } else {
            World.destroyBlock(Pos, true, Shadow);
        }
        double X = Pos.getX() + 0.5;
        double Y = Pos.getY() + 0.5;
        double Z = Pos.getZ() + 0.5;
        World.sendParticles(ParticleTypes.SMOKE, X, Y, Z, 6, 0.12, 0.12, 0.12, 0.01);
        World.sendParticles(Scp017Registry.Shade.get(), X, Y, Z, 8, 0.2, 0.2, 0.2, 0.02);
        World.playSound(null, X, Y, Z, Scp017Registry.Snuff.get(), SoundSource.HOSTILE, 0.7f, 0.9f + World.random.nextFloat() * 0.2f);
    }
}
