package net.rai.scp_unleashed.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ChargingRedstone implements WireDevice {
    public static final int ChargeCost = 200;
    public static final int ChargeRate = 4;

    private static final List<Charge> Charges = new ArrayList<>();
    public static final Charge Redstone = register(() -> Blocks.REDSTONE_BLOCK, ModBlocks.ChargedRedstone, ChargeCost);

    private final Charge Kind;
    private int Progress;

    public ChargingRedstone(Charge Kind) {
        this.Kind = Kind;
    }

    public static Charge register(Supplier<? extends Block> Source, Supplier<? extends Block> Result, int Cost) {
        Charge Entry = new Charge(Source, Result, Cost);
        Charges.add(Entry);
        return Entry;
    }

    public static List<Charge> charges() {
        return Collections.unmodifiableList(Charges);
    }

    @Nullable
    public static Charge chargeFor(BlockState State) {
        for (Charge Entry : Charges) {
            if (Entry.accepts(State)) {
                return Entry;
            }
        }
        return null;
    }

    public static boolean isChargeable(BlockState State) {
        return chargeFor(State) != null;
    }

    public static boolean convert(ServerLevel World, BlockPos Pos) {
        Charge Entry = chargeFor(World.getBlockState(Pos));
        if (Entry == null) {
            return false;
        }
        World.setBlockAndUpdate(Pos, Entry.Result().get().defaultBlockState());
        Vec3 Centre = Vec3.atCenterOf(Pos);
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Centre.x, Centre.y, Centre.z, 24, 0.45, 0.45, 0.45, 0.05);
        World.playSound(null, Pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.8f, 1.3f);
        return true;
    }

    public Charge getCharge() {
        return Kind;
    }

    public boolean isCharged() {
        return Progress >= Kind.Cost();
    }

    @Override
    public int getPowerDemand() {
        return Math.max(0, Math.min(ChargeRate, Kind.Cost() - Progress));
    }

    @Override
    public void insertPower(int Amount) {
        Progress = Math.min(Kind.Cost(), Progress + Amount);
    }

    @Override
    public int getStoredPower() {
        return Progress;
    }

    @Override
    public int getPowerCapacity() {
        return Kind.Cost();
    }

    public record Charge(Supplier<? extends Block> Source, Supplier<? extends Block> Result, int Cost) {
        public boolean accepts(BlockState State) {
            return State.is(Source.get());
        }
    }
}
