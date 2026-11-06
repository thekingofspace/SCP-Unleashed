package net.rai.scp_unleashed.fission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class ReactorCooling {
    public static final int Interval = 20;
    public static final float ExchangerRate = 0.000055f;
    public static final float IceRate = 0.0000045f;
    public static final float FrostedIceRate = 0.000003f;
    public static final float PackedIceRate = 0.0000065f;
    public static final float BlueIceRate = 0.0000095f;
    public static final float SnowRate = 0.000002f;
    public static final float WaterPerDegree = 32.0f;
    public static final float MeltStart = 400.0f;
    public static final float MeltSpan = 800.0f;
    public static final float MeltChance = 1.0f / 600.0f;
    public static final float ActiveDelta = 25.0f;

    private ReactorCooling() {
    }

    public record Survey(int Exchangers, int Flowing, int Ice, float IceCooling, float ExchangerCooling, float Flow) {
        public static final Survey Empty = new Survey(0, 0, 0, 0.0f, 0.0f, 0.0f);

        public float total() {
            return IceCooling + ExchangerCooling * Flow;
        }
    }

    public static float rateOf(BlockState Under) {
        if (Under.is(Blocks.BLUE_ICE)) {
            return BlueIceRate;
        }
        if (Under.is(Blocks.PACKED_ICE)) {
            return PackedIceRate;
        }
        if (Under.is(Blocks.ICE)) {
            return IceRate;
        }
        if (Under.is(Blocks.FROSTED_ICE)) {
            return FrostedIceRate;
        }
        if (Under.is(Blocks.SNOW_BLOCK) || Under.is(Blocks.POWDER_SNOW)) {
            return SnowRate;
        }
        return 0.0f;
    }

    public static float meltOdds(float Temperature) {
        float Heat = (Temperature - MeltStart) / MeltSpan;
        return Heat <= 0.0f ? 0.0f : Math.min(4.0f, Heat * Heat) * MeltChance;
    }

    public static Survey survey(ServerLevel World, BlockPos Origin, float Temperature) {
        float Delta = Math.max(0.0f, Temperature - ReactorThermal.Ambient);
        float Odds = meltOdds(Temperature);
        RandomSource Random = World.getRandom();
        List<HeatExchangerBlockEntity> Banks = new ArrayList<>();
        int Ice = 0;
        float IceCooling = 0.0f;
        for (int Z = 0; Z < ReactorLayout.Width; Z++) {
            for (int X = 0; X < ReactorLayout.Width; X++) {
                BlockPos Below = Origin.offset(X, -1, Z);
                BlockState Under = World.getBlockState(Below);
                if (Under.getBlock() instanceof HeatExchangerBlock && World.getBlockEntity(Below) instanceof HeatExchangerBlockEntity Bank) {
                    Banks.add(Bank);
                    continue;
                }
                float Rate = rateOf(Under);
                if (Rate <= 0.0f) {
                    continue;
                }
                Ice++;
                IceCooling += Rate;
                if (Odds > 0.0f && Random.nextFloat() < Odds) {
                    melt(World, Below, Under);
                }
            }
        }
        if (Banks.isEmpty()) {
            return new Survey(0, 0, Ice, IceCooling, 0.0f, 0.0f);
        }
        int Available = 0;
        int Flowing = 0;
        int[] Held = new int[Banks.size()];
        for (int Index = 0; Index < Banks.size(); Index++) {
            Held[Index] = Banks.get(Index).getWater();
            Available += Held[Index];
            if (Held[Index] > 0) {
                Flowing++;
            }
        }
        float Demand = Banks.size() * ExchangerRate * Delta * Interval * WaterPerDegree;
        float Flow;
        if (Available <= 0) {
            Flow = 0.0f;
        } else if (Demand < 1.0f) {
            Flow = 1.0f;
        } else {
            Flow = Math.min(1.0f, Available / Demand);
            int Remaining = (int) Math.ceil(Math.min(Demand, Available));
            for (int Index = 0; Index < Banks.size() && Remaining > 0; Index++) {
                if (Held[Index] <= 0) {
                    continue;
                }
                int Share = Math.min(Remaining, Math.min(Held[Index], (int) Math.ceil(Math.min(Demand, Available) * Held[Index] / (double) Available)));
                Remaining -= Banks.get(Index).drainWater(Share);
            }
        }
        if (Delta >= ActiveDelta && Flow > 0.0f) {
            long Now = World.getGameTime();
            for (int Index = 0; Index < Banks.size(); Index++) {
                if (Held[Index] > 0) {
                    Banks.get(Index).markCooling(Now);
                }
            }
        }
        return new Survey(Banks.size(), Flowing, Ice, IceCooling, Banks.size() * ExchangerRate, Flow);
    }

    private static void melt(ServerLevel World, BlockPos Pos, BlockState State) {
        BlockState Next;
        if (State.is(Blocks.BLUE_ICE)) {
            Next = Blocks.PACKED_ICE.defaultBlockState();
        } else if (State.is(Blocks.PACKED_ICE)) {
            Next = Blocks.ICE.defaultBlockState();
        } else if (State.is(Blocks.SNOW_BLOCK) || State.is(Blocks.POWDER_SNOW) || World.dimensionType().ultraWarm()) {
            Next = Blocks.AIR.defaultBlockState();
        } else {
            Next = IceBlock.meltsInto();
        }
        World.setBlockAndUpdate(Pos, Next);
        World.playSound(null, Pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.35f, 1.4f + World.getRandom().nextFloat() * 0.3f);
        World.sendParticles(ParticleTypes.CLOUD, Pos.getX() + 0.5, Pos.getY() + 0.6, Pos.getZ() + 0.5, 4, 0.35, 0.15, 0.35, 0.01);
    }
}
