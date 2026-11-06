package net.rai.scp_unleashed.scp428;

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
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.archive.investigation.SiteCondition;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scp428SiteCondition(int MinDistance, int MaxDistance, int Accuracy, int Arrival) implements StartCondition {
    public static final String TypeName = "scp428_site";
    public static final int PlanAttempts = 24;
    public static final MapCodec<Scp428SiteCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(16, 30000).optionalFieldOf("min_distance", 200).forGetter(Scp428SiteCondition::MinDistance),
            Codec.intRange(16, 30000).optionalFieldOf("max_distance", 600).forGetter(Scp428SiteCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 24).forGetter(Scp428SiteCondition::Accuracy),
            Codec.intRange(4, 256).optionalFieldOf("arrival", 40).forGetter(Scp428SiteCondition::Arrival)
    ).apply(Instance, Scp428SiteCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private SiteCondition site() {
        return new SiteCondition(MinDistance, MaxDistance, Accuracy, Arrival);
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        BlockPos Target = plan(World, Reader.blockPosition(), Reader.getRandom());
        CompoundTag State = Case.getState();
        String Dimension = LeadPlanner.dimensionOf(World);
        State.putString("Dimension", Dimension);
        State.putInt("X", Target.getX());
        State.putInt("Z", Target.getZ());
        Case.markDirty();
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable("archive.scp_unleashed.notes.scp428.0"));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site", LeadPlanner.roughly(Target.getX(), Accuracy, Reader.getRandom()),
                LeadPlanner.roughly(Target.getZ(), Accuracy, Reader.getRandom())));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site.radius", Accuracy + Arrival));
        if (!World.dimension().equals(Level.OVERWORLD)) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.dimension", LeadPlanner.dimensionName(Dimension)));
        }
        return Lines;
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (!ReaderTargets.isReady(Reader)) {
            return Optional.empty();
        }
        return site().tick(Reader, Case);
    }

    private BlockPos plan(ServerLevel World, BlockPos Origin, RandomSource Dice) {
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < PlanAttempts; Attempt++) {
            BlockPos Target = LeadPlanner.pickTarget(World, Origin, MinDistance, MaxDistance, Dice);
            if (Fallback == null) {
                Fallback = Target;
            }
            if (dryLand(World, Target)) {
                return Target;
            }
        }
        return Fallback;
    }

    private static boolean dryLand(ServerLevel World, BlockPos Target) {
        Holder<Biome> Found = World.getUncachedNoiseBiome(QuartPos.fromBlock(Target.getX()), QuartPos.fromBlock(World.getSeaLevel()), QuartPos.fromBlock(Target.getZ()));
        return !Found.is(BiomeTags.IS_OCEAN) && !Found.is(BiomeTags.IS_DEEP_OCEAN) && !Found.is(BiomeTags.IS_RIVER);
    }
}
