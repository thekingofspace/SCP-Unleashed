package net.rai.scp_unleashed.archive.investigation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.notice.Notices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record SiteCondition(int MinDistance, int MaxDistance, int Accuracy, int Arrival) implements StartCondition {
    public static final String TypeName = "site";
    public static final int NearDistance = 160;
    public static final MapCodec<SiteCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(16, 30000).optionalFieldOf("min_distance", 300).forGetter(SiteCondition::MinDistance),
            Codec.intRange(16, 30000).optionalFieldOf("max_distance", 1500).forGetter(SiteCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 24).forGetter(SiteCondition::Accuracy),
            Codec.intRange(4, 256).optionalFieldOf("arrival", 40).forGetter(SiteCondition::Arrival)
    ).apply(Instance, SiteCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        BlockPos Target = LeadPlanner.pickTarget(World, Reader.blockPosition(), MinDistance, MaxDistance, Reader.getRandom());
        CompoundTag State = Case.getState();
        String Dimension = LeadPlanner.dimensionOf(World);
        State.putString("Dimension", Dimension);
        State.putInt("X", Target.getX());
        State.putInt("Z", Target.getZ());
        Case.markDirty();
        int ShownX = LeadPlanner.roughly(Target.getX(), Accuracy, Reader.getRandom());
        int ShownZ = LeadPlanner.roughly(Target.getZ(), Accuracy, Reader.getRandom());
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site", ShownX, ShownZ));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site.radius", Accuracy + Arrival));
        if (!World.dimension().equals(Level.OVERWORLD)) {
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
        int X = State.getInt("X");
        int Z = State.getInt("Z");
        double Distance = LeadPlanner.horizontalDistance(Reader, X, Z);
        if (Distance <= Arrival) {
            if (!Reader.serverLevel().isLoaded(new BlockPos(X, Reader.getBlockY(), Z))) {
                return Optional.empty();
            }
            return Optional.of(new Finding(LeadPlanner.surface(Reader.serverLevel(), X, Z, Reader.getBlockY()), false));
        }
        if (Distance <= NearDistance && !State.getBoolean("Near")) {
            State.putBoolean("Near", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.site_near"), true);
            LeadPlanner.playTo(Reader, ArchiveRegistry.StaticBurst, LeadPlanner.towards(Reader, X + 0.5, Z + 0.5, 6.0), 0.45f, 0.8f);
        }
        return Optional.empty();
    }
}
