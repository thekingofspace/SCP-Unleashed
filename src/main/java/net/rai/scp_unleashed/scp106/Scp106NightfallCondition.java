package net.rai.scp_unleashed.scp106;

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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.NightfallCondition;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp106NightfallCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp106_nightfall";
    public static final String NightKey = "Night";
    public static final String ReadyKey = "Ready";
    public static final int SearchAttempts = 40;
    public static final int Vertical = 4;
    public static final MapCodec<Scp106NightfallCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp106NightfallCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 300).forGetter(Scp106NightfallCondition::MaxDelay),
            Codec.intRange(3, 48).optionalFieldOf("min_distance", 6).forGetter(Scp106NightfallCondition::MinDistance),
            Codec.intRange(3, 48).optionalFieldOf("max_distance", 12).forGetter(Scp106NightfallCondition::MaxDistance)
    ).apply(Instance, Scp106NightfallCondition::new));

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
        Case.getState().putLong(ReadyKey, Reader.serverLevel().getGameTime() + rollDelay(Reader.getRandom()));
        Case.markDirty();
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        schedule(Reader, Case);
        Case.getState().putLong(NightKey, NightfallCondition.nextNightDay(Reader.serverLevel()));
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp106.0"),
                Component.translatable("archive.scp_unleashed.notes.scp106.1"),
                Component.translatable("archive.scp_unleashed.notes.scp106.2"));
    }

    public static boolean isNight(ServerLevel World) {
        if (!World.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            return true;
        }
        long TimeOfDay = World.getDayTime() % NightfallCondition.DayLength;
        return TimeOfDay >= NightfallCondition.DuskTime && TimeOfDay <= NightfallCondition.DawnTime;
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        if (World.dimensionType().hasFixedTime() || Reader.isSpectator() || Scp106Pocket.inPocket(World)) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        if (!State.contains(ReadyKey)) {
            schedule(Reader, Case);
            return Optional.empty();
        }
        if (World.getGameTime() < State.getLong(ReadyKey) || !(isNight(World) || Reader.isSleeping()) || !NightfallCondition.nightArrived(World, Case, NightKey)) {
            return Optional.empty();
        }
        return emergeSpot(World, Reader.blockPosition(), Reader.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public Optional<BlockPos> emergeSpot(ServerLevel World, BlockPos Centre, RandomSource Random) {
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Low + Random.nextDouble() * (High - Low);
            BlockPos Column = Centre.offset(Mth.floor(Math.cos(Angle) * Radius), 0, Mth.floor(Math.sin(Angle) * Radius));
            if (!World.isLoaded(Column)) {
                continue;
            }
            for (int Dy = 0; Dy <= Vertical * 2; Dy++) {
                int Offset = Dy % 2 == 0 ? Dy / 2 : -(Dy + 1) / 2;
                BlockPos Feet = Column.above(Offset);
                if (Scp106OldMan.openFor(World, Feet)) {
                    return Optional.of(Feet.immutable());
                }
            }
        }
        return Optional.empty();
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Pos = Found.Pos();
        Vec3 Spot = Vec3.atBottomCenterOf(Pos);
        Vec3 Toward = Found.Investigator().position().subtract(Spot);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        return Scp106Spawning.emerge(World, Spot, Yaw, MobSpawnType.EVENT) != null;
    }
}
