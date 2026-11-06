package net.rai.scp_unleashed.cooler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.fluiddump.PollutionRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CoolerFreeze {
    public static final int Interval = 5;
    public static final int Budget = 400;
    public static final int Rest = 60;
    public static final float Volume = 0.45f;
    private static final List<BlockPos> Full = offsets(CoolerBlockEntity.Radius);
    private static final List<BlockPos> Halved = offsets(CoolerBlockEntity.Radius * CoolerBlockEntity.HotShare);

    private CoolerFreeze() {
    }

    private static List<BlockPos> offsets(double Radius) {
        int Reach = (int) Math.ceil(Radius);
        List<BlockPos> Found = new ArrayList<>();
        for (int X = -Reach; X <= Reach; X++) {
            for (int Y = -Reach; Y <= Reach; Y++) {
                for (int Z = -Reach; Z <= Reach; Z++) {
                    if ((X != 0 || Y != 0 || Z != 0) && X * X + Y * Y + Z * Z <= Radius * Radius) {
                        Found.add(new BlockPos(X, Y, Z));
                    }
                }
            }
        }
        Found.sort(Comparator.comparingInt(Each -> Each.getX() * Each.getX() + Each.getY() * Each.getY() + Each.getZ() * Each.getZ()));
        return List.copyOf(Found);
    }

    public static List<BlockPos> reach(CoolerClimate Climate) {
        return Climate == CoolerClimate.Hot ? Halved : Full;
    }

    public static boolean isStillWater(BlockState State) {
        FluidState Liquid = State.getFluidState();
        return State.is(Blocks.WATER) && Liquid.isSource() && Liquid.is(FluidTags.WATER);
    }

    public static boolean freeze(ServerLevel World, BlockPos Target, BlockPos Source) {
        BlockState Old = World.getBlockState(Target);
        if (!isStillWater(Old) || PollutionRules.isProtected(World)) {
            return false;
        }
        BlockState Ice = EventHooks.fireFluidPlaceBlockEvent(World, Target, Source, Blocks.ICE.defaultBlockState());
        if (Ice == Old) {
            return false;
        }
        World.setBlockAndUpdate(Target, Ice);
        World.sendParticles(ParticleTypes.SNOWFLAKE, Target.getX() + 0.5, Target.getY() + 1.02, Target.getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.01);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), Target.getX() + 0.5, Target.getY() + 0.9, Target.getZ() + 0.5, 4, 0.3, 0.05, 0.3, 0.02);
        float Pitch = 0.9f + World.getRandom().nextFloat() * 0.2f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : CoolerRegistry.FreezeLayers) {
            World.playSound(null, Target, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
        return true;
    }
}
