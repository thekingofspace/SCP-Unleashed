package net.rai.scp_unleashed.scp075;

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
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.ArchiveRules;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.NightfallCondition;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp075NightfallCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance, int MinGroup, int MaxGroup) implements StartCondition {
    public static final String TypeName = "scp075_nightfall";
    public static final String ReadyKey = "Ready";
    public static final String DuskKey = "Dusk";
    public static final String NightKey = "Night";
    public static final int DuskMin = 200;
    public static final int DuskSpread = 600;
    public static final MapCodec<Scp075NightfallCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp075NightfallCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 300).forGetter(Scp075NightfallCondition::MaxDelay),
            Codec.intRange(8, 64).optionalFieldOf("min_distance", 16).forGetter(Scp075NightfallCondition::MinDistance),
            Codec.intRange(8, 64).optionalFieldOf("max_distance", 32).forGetter(Scp075NightfallCondition::MaxDistance),
            Codec.intRange(1, 6).optionalFieldOf("min_group", 2).forGetter(Scp075NightfallCondition::MinGroup),
            Codec.intRange(1, 6).optionalFieldOf("max_group", 3).forGetter(Scp075NightfallCondition::MaxGroup)
    ).apply(Instance, Scp075NightfallCondition::new));

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

    public int rollGroup(RandomSource Random) {
        int Low = Math.min(MinGroup, MaxGroup);
        int High = Math.max(MinGroup, MaxGroup);
        return Low + Random.nextInt(High - Low + 1);
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        schedule(Reader, Case);
        Case.getState().putLong(NightKey, NightfallCondition.nextNightDay(Reader.serverLevel()));
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp075.0"),
                Component.translatable("archive.scp_unleashed.notes.scp075.1"),
                Component.translatable("archive.scp_unleashed.notes.scp075.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        if (World.dimensionType().hasFixedTime() || !World.dimensionType().hasSkyLight() || Reader.isSpectator()) {
            return Optional.empty();
        }
        Scp075World Record = Scp075World.get(World.getServer());
        if (Record.hasArrived() && !ArchiveRules.repeatable(World.getServer())) {
            return Optional.of(new Finding(Record.spot().orElse(Reader.blockPosition()), false));
        }
        CompoundTag State = Case.getState();
        if (!State.contains(ReadyKey)) {
            schedule(Reader, Case);
            return Optional.empty();
        }
        long Now = World.getGameTime();
        boolean Night = Scp075Night.countsAsNight(World) || Reader.isSleeping();
        if (Night && !NightfallCondition.nightArrived(World, Case, NightKey)) {
            return Optional.empty();
        }
        if (!Night) {
            if (State.contains(DuskKey)) {
                State.remove(DuskKey);
                Case.markDirty();
            }
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
        return Scp075Spawning.surfaceSpot(World, Reader.blockPosition(), MinDistance, MaxDistance, Reader.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        Scp075World Record = Scp075World.get(World.getServer());
        if (Record.hasArrived() && !ArchiveRules.repeatable(World.getServer())) {
            return true;
        }
        BlockPos Pos = Found.Pos();
        Vec3 Spot = Vec3.atBottomCenterOf(Pos);
        Vec3 Toward = Found.Investigator().position().subtract(Spot);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        int Count = Found.Document().Condition() instanceof Scp075NightfallCondition Rule ? Rule.rollGroup(World.getRandom()) : 2;
        int Spawned = Scp075Spawning.spawnGroup(World, Spot, Count, Yaw, MobSpawnType.EVENT);
        if (Spawned <= 0) {
            return false;
        }
        Record.markArrived(Pos);
        return true;
    }
}
