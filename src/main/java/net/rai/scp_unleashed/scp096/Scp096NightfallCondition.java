package net.rai.scp_unleashed.scp096;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

public record Scp096NightfallCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp096_nightfall";
    public static final String ReadyKey = "Ready";
    public static final String DuskKey = "Dusk";
    public static final String NightKey = "Night";
    public static final int DuskMin = 200;
    public static final int DuskSpread = 900;
    public static final MapCodec<Scp096NightfallCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp096NightfallCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 300).forGetter(Scp096NightfallCondition::MaxDelay),
            Codec.intRange(8, 64).optionalFieldOf("min_distance", 24).forGetter(Scp096NightfallCondition::MinDistance),
            Codec.intRange(8, 64).optionalFieldOf("max_distance", 48).forGetter(Scp096NightfallCondition::MaxDistance)
    ).apply(Instance, Scp096NightfallCondition::new));

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
        Case.markDirty();
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp096.0"),
                Component.translatable("archive.scp_unleashed.notes.scp096.1"),
                Component.translatable("archive.scp_unleashed.notes.scp096.2"));
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
        if (!State.contains(ReadyKey)) {
            schedule(Reader, Case);
            return Optional.empty();
        }
        long Now = World.getGameTime();
        boolean Night = isNight(World) || Reader.isSleeping();
        if (!Night) {
            if (State.contains(DuskKey)) {
                State.remove(DuskKey);
                Case.markDirty();
            }
            return Optional.empty();
        }
        if (!NightfallCondition.nightArrived(World, Case, NightKey)) {
            return Optional.empty();
        }
        if (!State.contains(DuskKey)) {
            State.putLong(DuskKey, Now);
            if (State.getLong(ReadyKey) < Now + DuskMin) {
                State.putLong(ReadyKey, Now + DuskMin + Reader.getRandom().nextInt(DuskSpread + 1));
            }
            Case.markDirty();
        }
        if (Reader.isSleeping() && State.getLong(ReadyKey) > Now) {
            State.putLong(ReadyKey, Now);
            Case.markDirty();
        }
        if (Now < State.getLong(ReadyKey)) {
            return Optional.empty();
        }
        return Scp096Spawning.darkSpot(World, Reader.blockPosition(), MinDistance, MaxDistance, Reader.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        BlockPos Pos = Found.Pos();
        Vec3 Spot = Vec3.atBottomCenterOf(Pos);
        float Yaw = Scp096Spawning.awayFrom(Spot, Found.Investigator().position());
        return Scp096Spawning.place(Found.World(), Spot, Yaw, MobSpawnType.EVENT).isPresent();
    }
}
