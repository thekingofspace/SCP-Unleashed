package net.rai.scp_unleashed.scp082;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp082ShoreCondition(int MinDelay, int CheckInterval, float Chance, int MinShore, int MaxShore, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp082_shore";
    public static final long StepCap = 40L;
    public static final long DayLength = 24000L;
    public static final long DuskTime = 12000L;
    public static final int[] SampleRadii = {8, 16};
    public static final int SampleDirections = 8;
    public static final MapCodec<Scp082ShoreCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 10).forGetter(Scp082ShoreCondition::MinDelay),
            Codec.intRange(1, 600).optionalFieldOf("check_interval", 5).forGetter(Scp082ShoreCondition::CheckInterval),
            Codec.floatRange(0.0f, 1.0f).optionalFieldOf("chance", 0.12f).forGetter(Scp082ShoreCondition::Chance),
            Codec.intRange(0, 3600).optionalFieldOf("min_shore", 20).forGetter(Scp082ShoreCondition::MinShore),
            Codec.intRange(1, 7200).optionalFieldOf("max_shore", 150).forGetter(Scp082ShoreCondition::MaxShore),
            Codec.intRange(8, 96).optionalFieldOf("min_distance", 20).forGetter(Scp082ShoreCondition::MinDistance),
            Codec.intRange(8, 96).optionalFieldOf("max_distance", 44).forGetter(Scp082ShoreCondition::MaxDistance)
    ).apply(Instance, Scp082ShoreCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Case.getState().putLong("Ready", Reader.serverLevel().getGameTime() + MinDelay * 20L);
        Case.markDirty();
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp082.0"),
                Component.translatable("archive.scp_unleashed.notes.scp082.1"),
                Component.translatable("archive.scp_unleashed.notes.scp082.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        if (Reader.isSpectator() || Reader.isSleeping()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        long Now = World.getGameTime();
        if (!State.contains("Ready")) {
            State.putLong("Ready", Now + MinDelay * 20L);
            Case.markDirty();
            return Optional.empty();
        }
        long Last = State.getLong("Seen");
        State.putLong("Seen", Now);
        if (Now < State.getLong("Ready") || !isDaytime(World) || !nearShore(World, Reader.blockPosition())) {
            return Optional.empty();
        }
        long Step = Last > 0L ? Math.max(0L, Math.min(StepCap, Now - Last)) : 20L;
        long Shore = State.getLong("Shore") + Step;
        State.putLong("Shore", Shore);
        Case.markDirty();
        if (Shore < MinShore * 20L || Now < State.getLong("Next")) {
            return Optional.empty();
        }
        State.putLong("Next", Now + CheckInterval * 20L);
        boolean Due = Shore >= MaxShore * 20L || Reader.getRandom().nextFloat() < Chance;
        if (!Due) {
            return Optional.empty();
        }
        return Scp082Arrival.findSpot(World, Reader, MinDistance, Math.max(MinDistance, MaxDistance), Reader.getRandom()).map(Spot -> new Finding(Spot, false));
    }

    public static boolean isDaytime(ServerLevel World) {
        if (!World.dimensionType().hasSkyLight() || World.dimensionType().hasFixedTime()) {
            return false;
        }
        return World.getDayTime() % DayLength < DuskTime;
    }

    public static boolean isShoreBiome(LevelReader World, BlockPos Pos) {
        Holder<Biome> Found = World.getBiome(Pos);
        return Found.is(BiomeTags.IS_RIVER) || Found.is(BiomeTags.IS_BEACH);
    }

    public static boolean nearShore(ServerLevel World, BlockPos Centre) {
        if (isShoreBiome(World, Centre)) {
            return true;
        }
        for (int Radius : SampleRadii) {
            for (int Index = 0; Index < SampleDirections; Index++) {
                double Angle = Math.PI * 2.0 * Index / SampleDirections;
                BlockPos Sample = Centre.offset((int) Math.round(Math.cos(Angle) * Radius), 0, (int) Math.round(Math.sin(Angle) * Radius));
                if (World.isLoaded(Sample) && isShoreBiome(World, Sample)) {
                    return true;
                }
            }
        }
        return false;
    }
}
