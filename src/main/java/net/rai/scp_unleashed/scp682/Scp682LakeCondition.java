package net.rai.scp_unleashed.scp682;

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
import net.minecraft.world.level.biome.Biomes;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.SiteCondition;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scp682LakeCondition(int MinDistance, int MaxDistance, int Accuracy, int Arrival) implements StartCondition {
    public static final String TypeName = "scp682_deep_water";
    public static final String TriesKey = "WaterTries";
    public static final String NoteKey = "archive.scp_unleashed.notes.scp682.0";
    public static final int PlanAttempts = 32;
    public static final int GiveUpTries = 10;
    public static final MapCodec<Scp682LakeCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(16, 30000).optionalFieldOf("min_distance", 192).forGetter(Scp682LakeCondition::MinDistance),
            Codec.intRange(16, 30000).optionalFieldOf("max_distance", 480).forGetter(Scp682LakeCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 24).forGetter(Scp682LakeCondition::Accuracy),
            Codec.intRange(4, 256).optionalFieldOf("arrival", 40).forGetter(Scp682LakeCondition::Arrival)
    ).apply(Instance, Scp682LakeCondition::new));

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
        Lines.add(Component.translatable(NoteKey));
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
        Optional<Finding> Reached = site().tick(Reader, Case);
        if (Reached.isEmpty()) {
            return Reached;
        }
        ServerLevel World = Reader.serverLevel();
        CompoundTag State = Case.getState();
        BlockPos Site = new BlockPos(State.getInt("X"), Reader.getBlockY(), State.getInt("Z"));
        Optional<BlockPos> Water = Scp682Lake.find(World, Site, Scp682Lake.SearchRadius);
        if (Water.isPresent()) {
            return Optional.of(new Finding(Water.get(), false));
        }
        int Tries = State.getInt(TriesKey) + 1;
        State.putInt(TriesKey, Tries);
        Case.markDirty();
        if (Tries < GiveUpTries) {
            return Optional.empty();
        }
        return Reached;
    }

    private BlockPos plan(ServerLevel World, BlockPos Origin, RandomSource Random) {
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < PlanAttempts; Attempt++) {
            BlockPos Target = LeadPlanner.pickTarget(World, Origin, MinDistance, MaxDistance, Random);
            if (Fallback == null) {
                Fallback = Target;
            }
            if (wet(World, Target)) {
                return Target;
            }
        }
        return Fallback;
    }

    private static boolean wet(ServerLevel World, BlockPos Target) {
        Holder<Biome> Found = World.getUncachedNoiseBiome(QuartPos.fromBlock(Target.getX()), QuartPos.fromBlock(World.getSeaLevel()), QuartPos.fromBlock(Target.getZ()));
        return Found.is(BiomeTags.IS_OCEAN) || Found.is(BiomeTags.IS_DEEP_OCEAN) || Found.is(BiomeTags.IS_RIVER) || Found.is(Biomes.SWAMP) || Found.is(Biomes.MANGROVE_SWAMP);
    }
}
