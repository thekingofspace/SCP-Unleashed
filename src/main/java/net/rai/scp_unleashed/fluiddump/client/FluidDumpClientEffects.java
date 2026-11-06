package net.rai.scp_unleashed.fluiddump.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.fluiddump.DumpMode;
import net.rai.scp_unleashed.fluiddump.FluidDumpBlockEntity;
import net.rai.scp_unleashed.fluiddump.FluidDumpRegistry;
import net.rai.scp_unleashed.fluiddump.PollutionMath;
import net.rai.scp_unleashed.fluiddump.PollutionRules;
import net.rai.scp_unleashed.particle.PipeLiquidOptions;
import org.joml.Vector3f;

public final class FluidDumpClientEffects {
    public static final int BubbleTicks = 24;
    public static final int GurgleChance = 4;
    public static final float PlumeChance = 0.35f;
    public static final float BubbleChance = 0.6f;
    public static final float DripChance = 0.5f;
    public static final DustParticleOptions Silt = new DustParticleOptions(new Vector3f(((PollutionMath.Murk >> 16) & 255) / 255.0f,
            ((PollutionMath.Murk >> 8) & 255) / 255.0f, (PollutionMath.Murk & 255) / 255.0f), 1.4f);

    private FluidDumpClientEffects() {
    }

    public static void dumpTick(FluidDumpBlockEntity Dump) {
        Level World = Dump.getLevel();
        if (World == null || Minecraft.getInstance().level != World) {
            return;
        }
        DumpMode Mode = Dump.getMode();
        if (!Mode.isWorking()) {
            return;
        }
        BlockPos Pos = Dump.getBlockPos();
        RandomSource Random = World.getRandom();
        if (Mode.isSubmerged()) {
            submerged(World, Pos, Mode, Random);
        } else if (Mode == DumpMode.Pouring) {
            pouring(World, Pos, Dump.getHandling(), Random);
        }
    }

    private static void submerged(Level World, BlockPos Pos, DumpMode Mode, RandomSource Random) {
        boolean Bubbled = false;
        for (Direction Side : Direction.values()) {
            BlockPos Next = Pos.relative(Side);
            if (!PollutionRules.isOpenWater(World.getBlockState(Next))) {
                continue;
            }
            Bubbled = true;
            double X = Pos.getX() + 0.5 + Side.getStepX() * 0.56 + (Random.nextDouble() - 0.5) * (Side.getStepX() == 0 ? 0.7 : 0.1);
            double Y = Pos.getY() + 0.5 + Side.getStepY() * 0.56 + (Random.nextDouble() - 0.5) * (Side.getStepY() == 0 ? 0.7 : 0.1);
            double Z = Pos.getZ() + 0.5 + Side.getStepZ() * 0.56 + (Random.nextDouble() - 0.5) * (Side.getStepZ() == 0 ? 0.7 : 0.1);
            if (Random.nextFloat() < BubbleChance) {
                World.addParticle(ParticleTypes.BUBBLE, X, Y, Z, Side.getStepX() * 0.05, 0.08 + Random.nextDouble() * 0.05, Side.getStepZ() * 0.05);
            }
            if (Mode == DumpMode.Polluting && Random.nextFloat() < PlumeChance) {
                World.addParticle(Silt, X, Y, Z, Side.getStepX() * 0.04, 0.02, Side.getStepZ() * 0.04);
            }
            if (Mode == DumpMode.Quenching && Random.nextFloat() < PlumeChance) {
                World.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, X, Y, Z, 0.0, 0.1, 0.0);
            }
        }
        if (!Bubbled || Math.floorMod(World.getGameTime() + Pos.asLong(), BubbleTicks) != 0) {
            return;
        }
        double X = Pos.getX() + 0.5;
        double Y = Pos.getY() + 0.5;
        double Z = Pos.getZ() + 0.5;
        World.playLocalSound(X, Y, Z, FluidDumpRegistry.DumpBubble.get(), SoundSource.BLOCKS, 0.55f, 0.9f + Random.nextFloat() * 0.15f, false);
        if (Random.nextInt(GurgleChance) == 0) {
            World.playLocalSound(X, Y, Z, FluidDumpRegistry.DumpGurgle.get(), SoundSource.BLOCKS, 0.45f, 0.85f + Random.nextFloat() * 0.2f, false);
        }
    }

    private static void pouring(Level World, BlockPos Pos, Fluid Liquid, RandomSource Random) {
        if (Liquid == Fluids.EMPTY || Random.nextFloat() >= DripChance) {
            return;
        }
        PipeLiquidOptions Drop = new PipeLiquidOptions(Liquid, false);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            BlockPos Next = Pos.relative(Side);
            if (!World.getBlockState(Next).getCollisionShape(World, Next).isEmpty()) {
                continue;
            }
            double X = Pos.getX() + 0.5 + Side.getStepX() * 0.54 + (Random.nextDouble() - 0.5) * (Side.getStepX() == 0 ? 0.6 : 0.05);
            double Z = Pos.getZ() + 0.5 + Side.getStepZ() * 0.54 + (Random.nextDouble() - 0.5) * (Side.getStepZ() == 0 ? 0.6 : 0.05);
            World.addParticle(Drop, X, Pos.getY() + 0.2 + Random.nextDouble() * 0.2, Z, Side.getStepX() * 0.03, -0.05, Side.getStepZ() * 0.03);
        }
    }
}
