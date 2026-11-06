package net.rai.scp_unleashed.gas.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.VaporizerBlock;
import net.rai.scp_unleashed.gas.VaporizerBlockEntity;
import net.rai.scp_unleashed.particle.PipeLiquidOptions;
import net.rai.scp_unleashed.plumbing.DrainBlock;
import net.rai.scp_unleashed.plumbing.DrainBlockEntity;
import net.rai.scp_unleashed.plumbing.PlumbingRegistry;
import net.rai.scp_unleashed.pressure.GasPort;

public final class GasClientEffects {
    public static final float BoilChance = 0.035f;
    public static final float PuffChance = 0.25f;
    public static final int DropsPerTick = 3;
    public static final double DropSpread = 0.38;
    public static final int FlowTicks = 12;
    public static final int DripChance = 9;

    private GasClientEffects() {
    }

    private static boolean isLocal(Level World) {
        return World != null && Minecraft.getInstance().level == World;
    }

    public static void vaporizerTick(VaporizerBlockEntity Machine) {
        Level World = Machine.getLevel();
        if (!isLocal(World)) {
            return;
        }
        BlockState State = Machine.getBlockState();
        if (!(State.getBlock() instanceof VaporizerBlock) || !State.getValue(VaporizerBlock.Active)) {
            return;
        }
        BlockPos Pos = Machine.getBlockPos();
        RandomSource Random = World.getRandom();
        if (Random.nextFloat() < BoilChance) {
            World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, GasRegistry.VaporizerBoil.get(), SoundSource.BLOCKS, 0.45f, 0.9f + Random.nextFloat() * 0.2f, false);
        }
        BlockState Above = World.getBlockState(Pos.above());
        boolean Ducted = Above.getBlock() instanceof GasPort || !Above.isAir() && Above.isCollisionShapeFullBlock(World, Pos.above());
        if (!Ducted && Random.nextFloat() < PuffChance) {
            double X = Pos.getX() + 0.3 + Random.nextDouble() * 0.4;
            double Z = Pos.getZ() + 0.3 + Random.nextDouble() * 0.4;
            World.addParticle(ParticleTypes.CLOUD, X, Pos.getY() + 1.02, Z, 0.0, 0.03 + Random.nextDouble() * 0.02, 0.0);
        }
    }

    public static void drainTick(DrainBlockEntity Drain) {
        Level World = Drain.getLevel();
        if (!isLocal(World)) {
            return;
        }
        BlockState State = Drain.getBlockState();
        Fluid Liquid = Drain.getPouring();
        if (!(State.getBlock() instanceof DrainBlock) || !State.getValue(DrainBlock.Flowing) || Liquid == Fluids.EMPTY) {
            return;
        }
        BlockPos Pos = Drain.getBlockPos();
        RandomSource Random = World.getRandom();
        PipeLiquidOptions Drop = new PipeLiquidOptions(Liquid, false);
        for (int Index = 0; Index < DropsPerTick; Index++) {
            double X = Pos.getX() + 0.5 + (Random.nextDouble() * 2.0 - 1.0) * DropSpread;
            double Z = Pos.getZ() + 0.5 + (Random.nextDouble() * 2.0 - 1.0) * DropSpread;
            World.addParticle(Drop, X, Pos.getY() - 0.04, Z, (Random.nextDouble() - 0.5) * 0.02, -0.12 - Random.nextDouble() * 0.08, (Random.nextDouble() - 0.5) * 0.02);
        }
        boolean Flow = Math.floorMod(World.getGameTime() + Pos.asLong(), FlowTicks) == 0;
        boolean Drip = Random.nextInt(DripChance) == 0;
        if (Flow || Drip) {
            double Floor = Pos.getY() - DrainBlockEntity.reach(World, Pos);
            if (Flow) {
                World.playLocalSound(Pos.getX() + 0.5, Floor + 0.2, Pos.getZ() + 0.5, PlumbingRegistry.DrainFlow.get(), SoundSource.BLOCKS, 0.45f, 0.96f + Random.nextFloat() * 0.08f, false);
            }
            if (Drip) {
                World.playLocalSound(Pos.getX() + 0.5, Floor + 0.2, Pos.getZ() + 0.5, PlumbingRegistry.DrainPatter.get(), SoundSource.BLOCKS, 0.4f, 0.92f + Random.nextFloat() * 0.16f, false);
            }
        }
    }
}
