package net.rai.scp_unleashed.fluiddump;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.fluiddump.network.PollutionPatch;
import net.rai.scp_unleashed.fluiddump.network.PollutionSyncPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PollutionField extends SavedData {
    public static final String DataName = "scp_unleashed_pollution";
    public static final int MaxSources = 64;
    public static final int DamageInterval = 40;
    public static final long SyncGap = 60L;
    public static final double SyncRadiusStep = 0.75;
    public static final double SyncDensityStep = 0.025;
    public static final long FirstSurveyDelay = 200L;
    private static final SavedData.Factory<PollutionField> DataFactory = new SavedData.Factory<>(PollutionField::new, PollutionField::load);

    private final List<PollutionSource> Sources = new ArrayList<>();
    private boolean Reshaped;
    private long LastSync = Long.MIN_VALUE / 2;

    public static PollutionField get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public List<PollutionSource> getSources() {
        return Sources;
    }

    public boolean isEmpty() {
        return Sources.isEmpty();
    }

    @Nullable
    public PollutionSource feed(ServerLevel World, BlockPos Pos, double Buckets) {
        if (Buckets <= 0.0 || PollutionRules.isProtected(World)) {
            return null;
        }
        long Now = World.getGameTime();
        PollutionSource Target = nearest(Pos, PollutionMath.MergeRange);
        if (Target == null && Sources.size() >= MaxSources) {
            Target = nearest(Pos, Double.MAX_VALUE);
        }
        if (Target == null) {
            Target = new PollutionSource(Pos, 0.0, 0.0, Now, Now + FirstSurveyDelay);
            Sources.add(Target);
            Reshaped = true;
        }
        Target.feed(Buckets, Now);
        setDirty();
        return Target;
    }

    @Nullable
    public PollutionSource nearest(BlockPos Pos, double Range) {
        PollutionSource Best = null;
        double BestDistance = Range;
        for (PollutionSource Source : Sources) {
            double Distance = Source.distanceTo(Pos);
            if (Distance <= BestDistance) {
                BestDistance = Distance;
                Best = Source;
            }
        }
        return Best;
    }

    public double densityAt(double X, double Y, double Z) {
        double Total = 0.0;
        for (PollutionSource Source : Sources) {
            double Here = Source.densityAt(X, Y, Z);
            if (Here > 0.0) {
                Total = PollutionMath.combine(Total, Here);
            }
        }
        return Total;
    }

    public double densityAt(BlockPos Pos) {
        return densityAt(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5);
    }

    public int clear(@Nullable BlockPos Centre, double Range) {
        int Removed = 0;
        Iterator<PollutionSource> Walker = Sources.iterator();
        while (Walker.hasNext()) {
            PollutionSource Source = Walker.next();
            if (Centre == null || Source.distanceTo(Centre) <= Range) {
                Walker.remove();
                Removed++;
            }
        }
        if (Removed > 0) {
            Reshaped = true;
            setDirty();
        }
        return Removed;
    }

    public void tick(ServerLevel World) {
        long Now = World.getGameTime();
        if (!Sources.isEmpty()) {
            Iterator<PollutionSource> Walker = Sources.iterator();
            while (Walker.hasNext()) {
                PollutionSource Source = Walker.next();
                Source.advance(Now, PollutionMath.Step);
                if (PollutionMath.forgotten(Source.getLoad()) && !Source.isFeeding(Now)) {
                    Walker.remove();
                    Reshaped = true;
                }
            }
            setDirty();
            if (Math.floorMod(Now, DamageInterval) == 0) {
                hurtSwimmers(World);
            }
            for (PollutionSource Source : Sources) {
                if (Now >= Source.getNextSurvey() && Source.getDensity() >= PollutionMath.CoverDensity && !PollutionConversion.busy(World)) {
                    PollutionConversion.start(World, Source);
                }
            }
        }
        syncIfNeeded(World, Now);
    }

    private void syncIfNeeded(ServerLevel World, long Now) {
        boolean Drifted = false;
        for (PollutionSource Source : Sources) {
            if (Source.drifted(SyncRadiusStep, SyncDensityStep)) {
                Drifted = true;
                break;
            }
        }
        if (Reshaped || Drifted && Now - LastSync >= SyncGap) {
            broadcast(World);
            LastSync = Now;
        }
    }

    public void broadcast(ServerLevel World) {
        Reshaped = false;
        for (PollutionSource Source : Sources) {
            Source.markSent();
        }
        PacketDistributor.sendToPlayersInDimension(World, payload());
    }

    public void sendTo(ServerPlayer Viewer) {
        PacketDistributor.sendToPlayer(Viewer, payload());
    }

    public PollutionSyncPayload payload() {
        List<PollutionPatch> Patches = new ArrayList<>(Sources.size());
        for (PollutionSource Source : Sources) {
            Patches.add(Source.patch());
        }
        return new PollutionSyncPayload(Patches);
    }

    public static AABB bounds(PollutionSource Source) {
        BlockPos Centre = Source.getCentre();
        double Reach = Source.getRadius();
        double Vertical = PollutionMath.verticalReach(Reach);
        return new AABB(Centre.getX() + 0.5 - Reach, Centre.getY() + 0.5 - Vertical, Centre.getZ() + 0.5 - Reach,
                Centre.getX() + 0.5 + Reach, Centre.getY() + 0.5 + Vertical, Centre.getZ() + 0.5 + Reach);
    }

    private void hurtSwimmers(ServerLevel World) {
        Set<LivingEntity> Swimmers = new LinkedHashSet<>();
        for (PollutionSource Source : Sources) {
            if (Source.getDensity() < PollutionMath.DamageThreshold || Source.getRadius() <= 0.0) {
                continue;
            }
            Swimmers.addAll(World.getEntitiesOfClass(LivingEntity.class, bounds(Source), Target -> Target.isInWater() && !PollutionRules.isExempt(Target)));
        }
        if (Swimmers.isEmpty()) {
            return;
        }
        DamageSource Cause = World.damageSources().source(FluidDumpRegistry.PollutedWaterDamage);
        for (LivingEntity Target : Swimmers) {
            if (PollutionRules.inWasteWater(Target)) {
                continue;
            }
            float Amount = PollutionMath.damage(densityAt(Target.getX(), Target.getY() + Target.getBbHeight() * 0.4, Target.getZ()));
            if (Amount > 0.0f) {
                Target.hurt(Cause, Amount);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (PollutionSource Source : Sources) {
            Saved.add(Source.save());
        }
        Tag.put("Sources", Saved);
        return Tag;
    }

    public static PollutionField load(CompoundTag Data, HolderLookup.Provider Registries) {
        PollutionField Field = new PollutionField();
        ListTag Saved = Data.getList("Sources", CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size() && Field.Sources.size() < MaxSources; Index++) {
            Field.Sources.add(PollutionSource.load(Saved.getCompound(Index)));
        }
        return Field;
    }
}
