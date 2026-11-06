package net.rai.scp_unleashed.scp031;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.notice.Notices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scp031VillageCondition(int MaxDistance, int Accuracy, int Arrival, int FallbackMin, int FallbackMax, int MinCount, int MaxCount) implements StartCondition {
    public static final String TypeName = "scp031_village";
    public static final int NearDistance = 160;
    public static final MapCodec<Scp031VillageCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(128, 6000).optionalFieldOf("max_distance", 1600).forGetter(Scp031VillageCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 32).forGetter(Scp031VillageCondition::Accuracy),
            Codec.intRange(16, 160).optionalFieldOf("arrival", 64).forGetter(Scp031VillageCondition::Arrival),
            Codec.intRange(64, 6000).optionalFieldOf("fallback_min", 300).forGetter(Scp031VillageCondition::FallbackMin),
            Codec.intRange(64, 6000).optionalFieldOf("fallback_max", 800).forGetter(Scp031VillageCondition::FallbackMax),
            Codec.intRange(1, 6).optionalFieldOf("min_count", 2).forGetter(Scp031VillageCondition::MinCount),
            Codec.intRange(1, 6).optionalFieldOf("max_count", 3).forGetter(Scp031VillageCondition::MaxCount)
    ).apply(Instance, Scp031VillageCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        Optional<ChunkPos> Village = Scp031Villages.nearest(World, Reader.blockPosition(), MaxDistance);
        CompoundTag State = Case.getState();
        BlockPos Target;
        if (Village.isPresent()) {
            Target = Scp031Villages.locate(World, Village.get());
            State.putBoolean("Village", true);
            State.putInt("ChunkX", Village.get().x);
            State.putInt("ChunkZ", Village.get().z);
        } else {
            Target = LeadPlanner.pickTarget(World, Reader.blockPosition(), FallbackMin, FallbackMax, Reader.getRandom());
            State.putBoolean("Village", false);
        }
        String Dimension = LeadPlanner.dimensionOf(World);
        State.putString("Dimension", Dimension);
        State.putInt("X", Target.getX());
        State.putInt("Z", Target.getZ());
        Case.markDirty();
        int ShownX = LeadPlanner.roughly(Target.getX(), Accuracy, Reader.getRandom());
        int ShownZ = LeadPlanner.roughly(Target.getZ(), Accuracy, Reader.getRandom());
        List<Component> Lines = new ArrayList<>();
        boolean Overworld = World.dimension().equals(Level.OVERWORLD);
        if (Overworld) {
            Lines.add(Component.translatable(Village.isPresent() ? "archive.scp_unleashed.notes.scp031.village" : "archive.scp_unleashed.notes.scp031.wild"));
        }
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site", ShownX, ShownZ));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site.radius", Accuracy + Arrival));
        if (!Overworld) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.dimension", LeadPlanner.dimensionName(Dimension)));
        }
        return Lines;
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        CompoundTag State = Case.getState();
        if (!LeadPlanner.inDimension(Reader, State.getString("Dimension"))) {
            return Optional.empty();
        }
        ServerLevel World = Reader.serverLevel();
        int X = State.getInt("X");
        int Z = State.getInt("Z");
        double Distance = LeadPlanner.horizontalDistance(Reader, X, Z);
        if (Distance <= Arrival) {
            if (!World.hasChunk(X >> 4, Z >> 4)) {
                return Optional.empty();
            }
            BlockPos Centre = new BlockPos(X, Reader.getBlockY(), Z);
            if (State.getBoolean("Village")) {
                Optional<BlockPos> Real = Scp031Villages.centre(World, State.getInt("ChunkX"), State.getInt("ChunkZ"));
                if (Real.isPresent() && World.hasChunk(Real.get().getX() >> 4, Real.get().getZ() >> 4)) {
                    Centre = Real.get();
                }
            }
            return Optional.of(new Finding(LeadPlanner.surface(World, Centre.getX(), Centre.getZ(), Reader.getBlockY()), false));
        }
        if (Distance <= NearDistance && !State.getBoolean("Near")) {
            State.putBoolean("Near", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.site_near"), true);
            LeadPlanner.playTo(Reader, ArchiveRegistry.StaticBurst, LeadPlanner.towards(Reader, X + 0.5, Z + 0.5, 6.0), 0.45f, 0.8f);
        }
        return Optional.empty();
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        int Low = 2;
        int High = 3;
        if (Found.Document().Condition() instanceof Scp031VillageCondition Settings) {
            Low = Math.min(Settings.MinCount(), Settings.MaxCount());
            High = Math.max(Settings.MinCount(), Settings.MaxCount());
        }
        int Count = Low + Found.World().getRandom().nextInt(High - Low + 1);
        return Scp031Villages.populate(Found.World(), Found.Pos(), Found.Investigator(), Count) > 0;
    }
}
