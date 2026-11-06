package net.rai.scp_unleashed.scp346;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp346FlockCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp346_flock";
    public static final String ReadyKey = "Ready";
    public static final int FlockSize = 4;
    public static final long LenientAfter = 2400L;
    public static final MapCodec<Scp346FlockCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 30).forGetter(Scp346FlockCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 120).forGetter(Scp346FlockCondition::MaxDelay),
            Codec.intRange(8, 64).optionalFieldOf("min_distance", 16).forGetter(Scp346FlockCondition::MinDistance),
            Codec.intRange(8, 64).optionalFieldOf("max_distance", 40).forGetter(Scp346FlockCondition::MaxDistance)
    ).apply(Instance, Scp346FlockCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private long rollDelay(RandomSource Random) {
        int Low = Math.min(MinDelay, MaxDelay);
        int High = Math.max(MinDelay, MaxDelay);
        return (Low + (long) Random.nextInt(High - Low + 1)) * 20L;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Case.getState().putLong(ReadyKey, Reader.serverLevel().getGameTime() + rollDelay(Reader.getRandom()));
        Case.markDirty();
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp346.0"),
                Component.translatable("archive.scp_unleashed.notes.scp346.1"),
                Component.translatable("archive.scp_unleashed.notes.scp346.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (!ReaderTargets.isReady(Reader) || Reader.isSleeping()) {
            return Optional.empty();
        }
        ServerLevel World = Reader.serverLevel();
        CompoundTag State = Case.getState();
        long Now = World.getGameTime();
        if (!State.contains(ReadyKey)) {
            State.putLong(ReadyKey, Now + rollDelay(Reader.getRandom()));
            Case.markDirty();
            return Optional.empty();
        }
        long Ready = State.getLong(ReadyKey);
        if (Now < Ready) {
            return Optional.empty();
        }
        boolean Strict = Now - Ready < LenientAfter;
        return Scp346Flock.openSky(World, Reader.position(), MinDistance, MaxDistance, Reader.getRandom(), Strict).map(Pos -> new Finding(Pos, true));
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        BlockPos Pos = Found.Pos();
        Vec3 Spot = Vec3.atBottomCenterOf(Pos);
        return !Scp346Flock.spawnFlock(Found.World(), Spot, FlockSize, null, Found.Investigator().position(), true).isEmpty();
    }
}
