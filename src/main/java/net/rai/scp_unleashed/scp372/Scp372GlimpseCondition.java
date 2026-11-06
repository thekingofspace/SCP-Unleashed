package net.rai.scp_unleashed.scp372;

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
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp372GlimpseCondition(int MinDelay, int MaxDelay, int CheckInterval, double Distance, double Angle) implements StartCondition {
    public static final String TypeName = "scp372_glimpse";
    public static final String ReadyKey = "Ready";
    public static final String NextKey = "Next";
    public static final MapCodec<Scp372GlimpseCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 30).forGetter(Scp372GlimpseCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 90).forGetter(Scp372GlimpseCondition::MaxDelay),
            Codec.intRange(1, 600).optionalFieldOf("check_interval", 5).forGetter(Scp372GlimpseCondition::CheckInterval),
            Codec.doubleRange(4.0, 16.0).optionalFieldOf("distance", 9.0).forGetter(Scp372GlimpseCondition::Distance),
            Codec.doubleRange(30.0, 89.0).optionalFieldOf("angle", 50.0).forGetter(Scp372GlimpseCondition::Angle)
    ).apply(Instance, Scp372GlimpseCondition::new));

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
        Scp372Unlock.unlock(Reader);
        return List.of(Component.translatable("archive.scp_unleashed.notes.scp372.0"), Component.translatable("archive.scp_unleashed.notes.scp372.1"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (Reader.isSpectator() || !Reader.isAlive() || Reader.isSleeping() || !AnomalyRealm.allows(Reader.level())) {
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
        if (Now < State.getLong(ReadyKey) || Now < State.getLong(NextKey)) {
            return Optional.empty();
        }
        State.putLong(NextKey, Now + CheckInterval * 20L);
        Case.markDirty();
        if (Scp372Glimpses.blocked(Reader.server, Reader.server.overworld().getGameTime())) {
            return Optional.empty();
        }
        return Optional.of(new Finding(edgeOfSight(Reader), true));
    }

    public BlockPos edgeOfSight(ServerPlayer Reader) {
        Vec3 Look = Reader.getViewVector(1.0f);
        double Turn = Math.toRadians(Reader.getRandom().nextBoolean() ? Angle : -Angle);
        Vec3 Side = new Vec3(Look.x * Math.cos(Turn) - Look.z * Math.sin(Turn), 0.0, Look.x * Math.sin(Turn) + Look.z * Math.cos(Turn));
        Vec3 Spot = Reader.getEyePosition().add(Side.normalize().scale(Distance));
        return BlockPos.containing(Spot.x, Reader.getY(), Spot.z);
    }
}
