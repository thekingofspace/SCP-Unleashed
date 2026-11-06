package net.rai.scp_unleashed.archive.investigation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.notice.Notices;

import java.util.List;
import java.util.Optional;

public record NightfallCondition(int Days, int MinRadius, int MaxRadius) implements StartCondition {
    public static final String TypeName = "nightfall";
    public static final long DayLength = 24000L;
    public static final long DuskTime = 12542L;
    public static final long DawnTime = 23460L;
    public static final int OmenChance = 150;
    public static final MapCodec<NightfallCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 60).optionalFieldOf("days", 2).forGetter(NightfallCondition::Days),
            Codec.intRange(3, 48).optionalFieldOf("min_radius", 10).forGetter(NightfallCondition::MinRadius),
            Codec.intRange(3, 64).optionalFieldOf("max_radius", 20).forGetter(NightfallCondition::MaxRadius)
    ).apply(Instance, NightfallCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    public static long nextNightDay(ServerLevel World) {
        long DayTime = World.getDayTime();
        long Day = DayTime / DayLength;
        return DayTime % DayLength < DuskTime ? Day : Day + 1L;
    }

    public static boolean nightArrived(ServerLevel World, Investigation Case, String Key) {
        if (!World.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            return true;
        }
        CompoundTag State = Case.getState();
        long Day = World.getDayTime() / DayLength;
        if (!State.contains(Key) || Day + 1L < State.getLong(Key)) {
            State.putLong(Key, nextNightDay(World));
            Case.markDirty();
        }
        return Day >= State.getLong(Key);
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        CompoundTag State = Case.getState();
        State.putLong("StartDay", Reader.level().getDayTime() / DayLength);
        State.putLong("OmenDay", -1L);
        Case.markDirty();
        return List.of(
                Component.translatable(Days == 0 ? "archive.scp_unleashed.notes.nightfall.tonight" : "archive.scp_unleashed.notes.nightfall", Days),
                Component.translatable("archive.scp_unleashed.notes.nightfall.advice"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        if (World.dimensionType().hasFixedTime() || Reader.isSpectator()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        long Day = World.getDayTime() / DayLength;
        if (Day < State.getLong("StartDay")) {
            State.putLong("StartDay", Day);
            Case.markDirty();
        }
        long Elapsed = Math.max(Day - State.getLong("StartDay"), (World.getGameTime() - Case.getStarted()) / DayLength);
        long TimeOfDay = World.getDayTime() % DayLength;
        boolean Cycle = World.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        boolean Dark = !Cycle || TimeOfDay >= DuskTime && TimeOfDay <= DawnTime;
        RandomSource Random = Reader.getRandom();
        if (Elapsed >= Days && (Dark || Reader.isSleeping())) {
            int Low = Math.min(MinRadius, MaxRadius);
            int High = Math.max(MinRadius, MaxRadius);
            Optional<BlockPos> Spot = LeadPlanner.standableNear(World, Reader.blockPosition(), Low, High, 6, Random);
            return Spot.map(Pos -> new Finding(Pos, true));
        }
        if (Elapsed >= Days - 1L && Dark && State.getLong("OmenDay") != Day && Random.nextInt(OmenChance) == 0) {
            State.putLong("OmenDay", Day);
            Case.markDirty();
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            Vec3 Where = Reader.position().add(Math.cos(Angle) * 14.0, 1.0, Math.sin(Angle) * 14.0);
            LeadPlanner.playTo(Reader, ArchiveRegistry.Omen, Where, 0.9f, 0.85f + Random.nextFloat() * 0.2f);
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.omen"), false);
        }
        return Optional.empty();
    }
}
