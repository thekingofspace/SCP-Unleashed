package net.rai.scp_unleashed.door;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DoorLeverBlockEntity extends BlockEntity {
    public static final float ThrowAngle = 40.0f;
    public static final int ThrowTicks = 6;
    public static final int HitTick = 3;
    public static final float Rebound = 0.14f;
    public static final int PredictionTicks = 20;

    private boolean Thrown;
    private float From;
    private float To;
    private float Angle;
    private float Previous;
    private int Age = ThrowTicks;
    private int Predicted = -1;
    private int PredictionAge;

    public DoorLeverBlockEntity(BlockPos Pos, BlockState State) {
        super(DoorRegistry.DoorLeverEntity.get(), Pos, State);
        Thrown = State.hasProperty(DoorLeverBlock.Powered) && State.getValue(DoorLeverBlock.Powered);
        Angle = restingAngle(Thrown);
        Previous = Angle;
        From = Angle;
        To = Angle;
    }

    public static float restingAngle(boolean On) {
        return On ? ThrowAngle : -ThrowAngle;
    }

    public static float throwCurve(float Progress) {
        float Hit = HitTick / (float) ThrowTicks;
        if (Progress <= Hit) {
            float Phase = Progress / Hit;
            return Phase * Phase;
        }
        float Phase = Math.min(1.0f, (Progress - Hit) / (1.0f - Hit));
        return 1.0f - Rebound * Mth.sin(Phase * Mth.PI) * (1.0f - Phase);
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, DoorLeverBlockEntity Lever) {
        if (State.getBlock() instanceof DoorLeverBlock) {
            Lever.tickClient(World, State.getValue(DoorLeverBlock.Powered));
        }
    }

    private void tickClient(Level World, boolean Actual) {
        if (Predicted >= 0 && ((Predicted == 1) == Actual || ++PredictionAge > PredictionTicks)) {
            Predicted = -1;
        }
        boolean Wanted = Predicted >= 0 ? Predicted == 1 : Actual;
        if (Wanted != Thrown) {
            Thrown = Wanted;
            From = Angle;
            To = restingAngle(Wanted);
            Age = 0;
            sound(World, DoorSounds.LeverRelease.get(), 0.55f, 1.0f);
        }
        Previous = Angle;
        if (Age < ThrowTicks) {
            Age++;
            Angle = From + (To - From) * throwCurve(Age / (float) ThrowTicks);
            if (Age == HitTick) {
                sound(World, Thrown ? DoorSounds.LeverOn.get() : DoorSounds.LeverOff.get(), 0.75f, 1.0f);
                sound(World, DoorSounds.LeverLatch.get(), Thrown ? 0.7f : 0.6f, Thrown ? 1.08f : 1.0f);
            }
        }
    }

    private void sound(Level World, SoundEvent Event, float Volume, float Pitch) {
        BlockPos Pos = getBlockPos();
        World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Event, SoundSource.BLOCKS, Volume, Pitch * (1.0f + 0.04f * World.getRandom().nextFloat()), false);
    }

    public void predict(boolean On) {
        Predicted = On ? 1 : 0;
        PredictionAge = 0;
    }

    public float getAngle(float PartialTick) {
        return Mth.lerp(PartialTick, Previous, Angle);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = super.getUpdateTag(Registries);
        Tag.putBoolean("Thrown", getBlockState().hasProperty(DoorLeverBlock.Powered) && getBlockState().getValue(DoorLeverBlock.Powered));
        return Tag;
    }
}
