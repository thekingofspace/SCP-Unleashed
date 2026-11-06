package net.rai.scp_unleashed.scp017;

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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.NightfallCondition;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp017NightfallCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp017_nightfall";
    public static final int SearchAttempts = 32;
    public static final int Vertical = 6;
    public static final int SkyPenalty = 3;
    public static final String NightKey = "Night";
    public static final MapCodec<Scp017NightfallCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp017NightfallCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 300).forGetter(Scp017NightfallCondition::MaxDelay),
            Codec.intRange(6, 64).optionalFieldOf("min_distance", 12).forGetter(Scp017NightfallCondition::MinDistance),
            Codec.intRange(6, 64).optionalFieldOf("max_distance", 28).forGetter(Scp017NightfallCondition::MaxDistance)
    ).apply(Instance, Scp017NightfallCondition::new));

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
        Case.getState().putLong(NightKey, NightfallCondition.nextNightDay(Reader.serverLevel()));
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp017.0"),
                Component.translatable("archive.scp_unleashed.notes.scp017.1"),
                Component.translatable("archive.scp_unleashed.notes.scp017.2"));
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
        if (World.dimensionType().hasFixedTime() || Reader.isSpectator()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        if (!State.contains("Ready")) {
            schedule(Reader, Case);
            return Optional.empty();
        }
        if (World.getGameTime() < State.getLong("Ready") || !(isNight(World) || Reader.isSleeping()) || !NightfallCondition.nightArrived(World, Case, NightKey)) {
            return Optional.empty();
        }
        return darkSpot(World, Reader.blockPosition(), Reader.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public Optional<BlockPos> darkSpot(ServerLevel World, BlockPos Centre, RandomSource Random) {
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        BlockPos Best = null;
        int BestScore = Integer.MAX_VALUE;
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Low + Random.nextDouble() * (High - Low);
            BlockPos Column = Centre.offset(Mth.floor(Math.cos(Angle) * Radius), 0, Mth.floor(Math.sin(Angle) * Radius));
            if (!World.isLoaded(Column)) {
                continue;
            }
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Cell = Column.above(Dy);
                if (!LeadPlanner.isDryStandable(World, Cell) || !roomy(World, Cell)) {
                    continue;
                }
                int Light = Scp017Light.at(World, Cell);
                if (Light > Scp017Light.Threshold) {
                    continue;
                }
                int Score = Light + (World.canSeeSky(Cell) ? SkyPenalty : 0);
                if (Score < BestScore) {
                    BestScore = Score;
                    Best = Cell.immutable();
                }
            }
        }
        return Optional.ofNullable(Best);
    }

    private static boolean roomy(ServerLevel World, BlockPos Cell) {
        AABB Box = Scp017Registry.Shadow.get().getDimensions().makeBoundingBox(new Vec3(Cell.getX() + 0.5, Cell.getY(), Cell.getZ() + 0.5));
        return World.noCollision(Box);
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        BlockPos Pos = Found.Pos();
        Vec3 Spot = new Vec3(Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5);
        Vec3 Toward = Found.Investigator().position().subtract(Spot);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        return Scp017Shadow.manifest(Found.World(), Spot, Yaw).isPresent();
    }
}
