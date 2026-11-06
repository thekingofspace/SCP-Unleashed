package net.rai.scp_unleashed.scp354;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.ArchiveRules;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.notice.Notices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scp354PoolCondition(int MinDistance, int MaxDistance, int Accuracy, int Arrival) implements StartCondition {
    public static final String TypeName = "scp354_pool";
    public static final int PlanAttempts = 48;
    public static final int NearAttempts = 16;
    public static final int CheckTicks = 20;
    public static final int SiteRadius = 12;
    public static final int SpreadStep = 12;
    public static final int MaxSpread = 48;
    public static final int StrictTries = 4;
    public static final int LenientTries = 10;
    public static final int NearDistance = 160;
    public static final int ForcedMin = 16;
    public static final int ForcedMax = 40;
    public static final double KnownSearch = 64.0;
    public static final MapCodec<Scp354PoolCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(16, 4000).optionalFieldOf("min_distance", 64).forGetter(Scp354PoolCondition::MinDistance),
            Codec.intRange(16, 4000).optionalFieldOf("max_distance", 160).forGetter(Scp354PoolCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 16).forGetter(Scp354PoolCondition::Accuracy),
            Codec.intRange(8, 256).optionalFieldOf("arrival", 48).forGetter(Scp354PoolCondition::Arrival)
    ).apply(Instance, Scp354PoolCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.server.overworld();
        boolean Here = Reader.serverLevel().dimension().equals(Level.OVERWORLD);
        BlockPos Origin = Here ? Reader.blockPosition() : World.getSharedSpawnPos();
        CompoundTag State = Case.getState();
        Optional<Scp354Pools.Pool> Existing = ArchiveRules.repeatable(Reader.server) ? Optional.empty() : Scp354Pools.get(Reader.server).first(Level.OVERWORLD);
        BlockPos Target;
        if (Existing.isPresent()) {
            Target = Existing.get().Centre;
            State.putBoolean("Known", true);
        } else {
            Target = plan(World, Origin, Reader.getRandom());
        }
        State.putString("Dimension", LeadPlanner.dimensionOf(World));
        State.putInt("X", Target.getX());
        State.putInt("Z", Target.getZ());
        State.putInt("Tries", 0);
        Case.markDirty();
        return notes(Reader, Target, Here);
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        CompoundTag State = Case.getState();
        if (!LeadPlanner.inDimension(Reader, State.getString("Dimension")) || Reader.isSpectator()) {
            return Optional.empty();
        }
        ServerLevel World = Reader.serverLevel();
        int X = State.getInt("X");
        int Z = State.getInt("Z");
        double Distance = LeadPlanner.horizontalDistance(Reader, X, Z);
        if (Distance <= NearDistance && !State.getBoolean("Near")) {
            State.putBoolean("Near", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp354_near"), false);
            LeadPlanner.playTo(Reader, Scp354Registry.PoolGurgle, LeadPlanner.towards(Reader, X + 0.5, Z + 0.5, 6.0), 0.4f, 0.6f);
        }
        long Now = World.getGameTime();
        if (Now < State.getLong("Next")) {
            return Optional.empty();
        }
        State.putLong("Next", Now + CheckTicks);
        BlockPos Column = new BlockPos(X, World.getSeaLevel(), Z);
        if (!World.isLoaded(Column)) {
            return Optional.empty();
        }
        Optional<Scp354Pools.Pool> Known = Scp354Pools.get(Reader.server).nearest(World.dimension(), Column, KnownSearch);
        if (Known.isPresent()) {
            return Optional.of(new Finding(Known.get().Centre, false));
        }
        if (State.getBoolean("Known")) {
            State.remove("Known");
            Case.markDirty();
        }
        int Tries = State.getInt("Tries");
        int Spread = Math.min(MaxSpread, SiteRadius + Tries * SpreadStep);
        int Mode = Tries < StrictTries ? Scp354Sites.Strict : Tries < LenientTries ? Scp354Sites.Lenient : Scp354Sites.Forced;
        Optional<BlockPos> Site = Scp354Sites.findSite(World, Reader, new Vec3(X + 0.5, Reader.getY(), Z + 0.5), 0, Spread, Mode, Reader.getRandom());
        if (Site.isPresent()) {
            return Site.map(Pos -> new Finding(Pos, false));
        }
        State.putInt("Tries", Tries + 1);
        Case.markDirty();
        if (Tries + 1 >= LenientTries && Distance <= Arrival) {
            return Scp354Sites.findSite(World, Reader, Reader.position(), ForcedMin, ForcedMax, Scp354Sites.Forced, Reader.getRandom()).map(Pos -> new Finding(Pos, false));
        }
        return Optional.empty();
    }

    private BlockPos plan(ServerLevel World, BlockPos Origin, RandomSource Random) {
        int Loaded = (World.getServer().getPlayerList().getViewDistance() + 2) * 16;
        int Span = Math.max(SiteRadius, MaxDistance - MinDistance);
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < PlanAttempts; Attempt++) {
            int Low = Attempt < NearAttempts ? MinDistance : Math.max(MinDistance, Loaded) + (Attempt - NearAttempts) / 16 * 32;
            int High = Attempt < NearAttempts ? MaxDistance : Low + Span;
            BlockPos Target = LeadPlanner.pickTarget(World, Origin, Low, High, Random);
            if (World.getChunkSource().hasChunk(Target.getX() >> 4, Target.getZ() >> 4)) {
                continue;
            }
            if (Fallback == null) {
                Fallback = Target;
            }
            if (isDryLand(World, Target)) {
                return Target;
            }
        }
        return Fallback != null ? Fallback : LeadPlanner.pickTarget(World, Origin, MinDistance, MaxDistance, Random);
    }

    private static boolean isDryLand(ServerLevel World, BlockPos Target) {
        Holder<Biome> Found = World.getUncachedNoiseBiome(QuartPos.fromBlock(Target.getX()), QuartPos.fromBlock(World.getSeaLevel()), QuartPos.fromBlock(Target.getZ()));
        return !Found.is(BiomeTags.IS_OCEAN) && !Found.is(BiomeTags.IS_DEEP_OCEAN) && !Found.is(BiomeTags.IS_RIVER) && !Found.is(BiomeTags.IS_BEACH);
    }

    private List<Component> notes(ServerPlayer Reader, BlockPos Target, boolean Here) {
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable("archive.scp_unleashed.notes.scp354.0"));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site", LeadPlanner.roughly(Target.getX(), Accuracy, Reader.getRandom()),
                LeadPlanner.roughly(Target.getZ(), Accuracy, Reader.getRandom())));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site.radius", Accuracy + Arrival));
        if (!Here) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.dimension", LeadPlanner.dimensionName(LeadPlanner.dimensionOf(Reader.server.overworld()))));
        }
        return Lines;
    }
}
