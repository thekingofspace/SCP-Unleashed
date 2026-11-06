package net.rai.scp_unleashed.scp323;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp323AppearanceCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp323_appearance";
    public static final int SearchAttempts = 48;
    public static final int Vertical = 8;
    public static final double ViewCone = 0.35;
    public static final double Jitter = 0.3;
    public static final MapCodec<Scp323AppearanceCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp323AppearanceCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 300).forGetter(Scp323AppearanceCondition::MaxDelay),
            Codec.intRange(6, 96).optionalFieldOf("min_distance", 20).forGetter(Scp323AppearanceCondition::MinDistance),
            Codec.intRange(6, 96).optionalFieldOf("max_distance", 40).forGetter(Scp323AppearanceCondition::MaxDistance)
    ).apply(Instance, Scp323AppearanceCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private long rollDelay(RandomSource Random) {
        int Low = Math.min(MinDelay, MaxDelay);
        int High = Math.max(MinDelay, MaxDelay);
        return (Low + (long) Random.nextInt(High - Low + 1)) * 20L;
    }

    private void schedule(ServerPlayer Reader, Investigation Case) {
        Case.getState().putLong("Ready", Reader.serverLevel().getGameTime() + rollDelay(Reader.getRandom()));
        Case.markDirty();
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        schedule(Reader, Case);
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp323.0"),
                Component.translatable("archive.scp_unleashed.notes.scp323.1"),
                Component.translatable("archive.scp_unleashed.notes.scp323.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (Reader.isSpectator()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        if (!State.contains("Ready")) {
            schedule(Reader, Case);
            return Optional.empty();
        }
        if (Reader.serverLevel().getGameTime() < State.getLong("Ready")) {
            return Optional.empty();
        }
        return hiddenSpot(Reader.serverLevel(), Reader, Reader.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public Optional<BlockPos> hiddenSpot(ServerLevel World, ServerPlayer Reader, RandomSource Random) {
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        Vec3 Eye = Reader.getEyePosition();
        Vec3 Look = Reader.getViewVector(1.0f);
        BlockPos Centre = Reader.blockPosition();
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Low + Random.nextDouble() * (High - Low);
            BlockPos Column = Centre.offset(Mth.floor(Math.cos(Angle) * Radius), 0, Mth.floor(Math.sin(Angle) * Radius));
            if (!World.isLoaded(Column)) {
                continue;
            }
            for (int Step = 0; Step <= Vertical * 2; Step++) {
                int Dy = Step % 2 == 0 ? Step / 2 : -(Step + 1) / 2;
                BlockPos Cell = Column.above(Dy);
                if (!LeadPlanner.isDryStandable(World, Cell)) {
                    continue;
                }
                Vec3 Point = new Vec3(Cell.getX() + 0.5, Cell.getY() + Scp323Skull.FaceLift, Cell.getZ() + 0.5);
                if (!Scp323Sight.clear(World, Eye, Point)) {
                    return Optional.of(Cell.immutable());
                }
                if (Fallback == null && Point.subtract(Eye).normalize().dot(Look) < ViewCone) {
                    Fallback = Cell.immutable();
                }
                break;
            }
        }
        return Optional.ofNullable(Fallback);
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        BlockPos Pos = Found.Pos();
        RandomSource Random = Found.World().random;
        Vec3 Spot = new Vec3(Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * Jitter, Pos.getY(), Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * Jitter);
        return Scp323Skull.place(Found.World(), Spot, Random.nextFloat() * 360.0f, false) != null;
    }
}
