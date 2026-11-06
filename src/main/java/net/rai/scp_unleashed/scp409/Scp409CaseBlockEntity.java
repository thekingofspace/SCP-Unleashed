package net.rai.scp_unleashed.scp409;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class Scp409CaseBlockEntity extends BlockEntity {
    public static final float LidSpeed = 1.0f / 14.0f;
    public static final int ExposureInterval = 5;
    public static final int HumOdds = 220;
    private float Openness;
    private float LastOpenness;
    private boolean Primed;

    public Scp409CaseBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp409Registry.CaseEntity.get(), Pos, State);
    }

    public float openness(float Partial) {
        float Raw = Mth.lerp(Partial, LastOpenness, Openness);
        return Raw * Raw * (3.0f - 2.0f * Raw);
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp409CaseBlockEntity Self) {
        float Target = Scp409CaseBlock.isOpen(State) ? 1.0f : 0.0f;
        if (!Self.Primed) {
            Self.Primed = true;
            Self.Openness = Target;
            Self.LastOpenness = Target;
            return;
        }
        Self.LastOpenness = Self.Openness;
        if (Self.Openness < Target) {
            Self.Openness = Math.min(Target, Self.Openness + LidSpeed);
        } else if (Self.Openness > Target) {
            Self.Openness = Math.max(Target, Self.Openness - LidSpeed);
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp409CaseBlockEntity Self) {
        if (!Scp409CaseBlock.isOpen(State) || !(World instanceof ServerLevel Server)) {
            return;
        }
        long Clock = Server.getGameTime() + Math.floorMod(Pos.hashCode(), ExposureInterval);
        if (Clock % ExposureInterval == 0L) {
            expose(Server, Pos, State);
        }
        if (Server.random.nextInt(HumOdds) == 0) {
            Server.playSound(null, Pos, Scp409Registry.Hum.get(), SoundSource.BLOCKS, 0.45f, 0.85f + Server.random.nextFloat() * 0.2f);
        }
    }

    public static int expose(ServerLevel World, BlockPos Pos, BlockState State) {
        List<LivingEntity> Near = World.getEntitiesOfClass(LivingEntity.class, Scp409CaseBlock.reachBox(State, Pos), LivingEntity::isAlive);
        int Caught = 0;
        for (LivingEntity Target : Near) {
            if (Scp409Exposure.touchCrystal(Target)) {
                Caught++;
            }
        }
        return Caught;
    }
}
