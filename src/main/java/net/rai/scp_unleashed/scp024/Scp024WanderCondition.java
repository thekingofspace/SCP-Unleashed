package net.rai.scp_unleashed.scp024;

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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.notice.Notices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scp024WanderCondition(int MinDistance, int MaxDistance, int Accuracy, int Arrival) implements StartCondition {
    public static final String TypeName = "scp024_wander";
    public static final int PlanAttempts = 48;
    public static final int CheckTicks = 20;
    public static final int SiteRadius = 16;
    public static final int SpreadStep = 16;
    public static final int MaxSpread = 48;
    public static final int StrictTries = 4;
    public static final int LenientTries = 10;
    public static final MapCodec<Scp024WanderCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(16, 4000).optionalFieldOf("min_distance", 96).forGetter(Scp024WanderCondition::MinDistance),
            Codec.intRange(16, 4000).optionalFieldOf("max_distance", 160).forGetter(Scp024WanderCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 24).forGetter(Scp024WanderCondition::Accuracy),
            Codec.intRange(8, 256).optionalFieldOf("arrival", 48).forGetter(Scp024WanderCondition::Arrival)
    ).apply(Instance, Scp024WanderCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        if (Reader.serverLevel().dimension() != Level.OVERWORLD) {
            return List.of(
                    Component.translatable("archive.scp_unleashed.notes.scp024.0"),
                    Component.translatable("archive.scp_unleashed.notes.scp024.1"),
                    Component.translatable("archive.scp_unleashed.notes.scp024.2"));
        }
        BlockPos Target = plan(Reader.serverLevel(), Reader.blockPosition(), Reader.getRandom());
        store(Case, Target);
        return notes(Reader, Target);
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        if (World.dimension() != Level.OVERWORLD || Reader.isSpectator()) {
            return Optional.empty();
        }
        BlockPos Door = Scp024World.get(Reader.server).door();
        if (Door != null) {
            return Optional.of(new Finding(Door, false));
        }
        CompoundTag State = Case.getState();
        if (!State.contains("X")) {
            BlockPos Target = plan(World, Reader.blockPosition(), Reader.getRandom());
            store(Case, Target);
            List<Component> Lines = notes(Reader, Target);
            Case.setNotes(Lines);
            Case.markDirty();
            rewriteNotes(Reader, Case, Lines);
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp024_traced"), false);
            return Optional.empty();
        }
        long Now = World.getGameTime();
        if (Now < State.getLong("Next")) {
            return Optional.empty();
        }
        State.putLong("Next", Now + CheckTicks);
        int X = State.getInt("X");
        int Z = State.getInt("Z");
        if (!World.isLoaded(new BlockPos(X, World.getSeaLevel(), Z))) {
            return Optional.empty();
        }
        int Tries = State.getInt("Tries");
        int Spread = Math.min(MaxSpread, SiteRadius + Tries * SpreadStep);
        int Mode = Tries < StrictTries ? Scp024Warehouse.Strict : Tries < LenientTries ? Scp024Warehouse.Lenient : Scp024Warehouse.Forced;
        Optional<BlockPos> Site = Scp024Warehouse.findSite(World, Reader, new Vec3(X + 0.5, Reader.getY(), Z + 0.5), 0, Spread, Mode, Reader.getRandom());
        if (Site.isPresent()) {
            return Site.map(Pos -> new Finding(Pos, false));
        }
        State.putInt("Tries", Tries + 1);
        Case.markDirty();
        if (Tries + 1 >= LenientTries && LeadPlanner.horizontalDistance(Reader, X, Z) <= Arrival) {
            return Scp024Warehouse.findSite(World, Reader, Reader.position(), 24, 48, Scp024Warehouse.Forced, Reader.getRandom()).map(Pos -> new Finding(Pos, false));
        }
        return Optional.empty();
    }

    private BlockPos plan(ServerLevel World, BlockPos Origin, RandomSource Random) {
        int Loaded = (World.getServer().getPlayerList().getViewDistance() + 2) * 16;
        int Low = Math.max(MinDistance, Loaded);
        int Span = Math.max(SiteRadius, MaxDistance - MinDistance);
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < PlanAttempts; Attempt++) {
            int Reach = Low + Attempt / 16 * 32;
            BlockPos Target = LeadPlanner.pickTarget(World, Origin, Reach, Reach + Span, Random);
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
        return Fallback != null ? Fallback : LeadPlanner.pickTarget(World, Origin, Low, Low + Span, Random);
    }

    private static boolean isDryLand(ServerLevel World, BlockPos Target) {
        Holder<Biome> Found = World.getUncachedNoiseBiome(QuartPos.fromBlock(Target.getX()), QuartPos.fromBlock(World.getSeaLevel()), QuartPos.fromBlock(Target.getZ()));
        return !Found.is(BiomeTags.IS_OCEAN) && !Found.is(BiomeTags.IS_DEEP_OCEAN) && !Found.is(BiomeTags.IS_RIVER) && !Found.is(BiomeTags.IS_BEACH);
    }

    private static void store(Investigation Case, BlockPos Target) {
        CompoundTag State = Case.getState();
        State.putInt("X", Target.getX());
        State.putInt("Z", Target.getZ());
        State.putInt("Tries", 0);
        Case.markDirty();
    }

    private List<Component> notes(ServerPlayer Reader, BlockPos Target) {
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site", LeadPlanner.roughly(Target.getX(), Accuracy, Reader.getRandom()),
                LeadPlanner.roughly(Target.getZ(), Accuracy, Reader.getRandom())));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site.radius", Accuracy + Arrival));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.scp024.1"));
        return Lines;
    }

    private static void rewriteNotes(ServerPlayer Reader, Investigation Case, List<Component> Lines) {
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Stack.is(ArchiveRegistry.OpenedDocument.get()) && Stamp != null && Stamp.Document().equals(Scp024Registry.DocumentId) && Notes != null && !Notes.Resolved()
                    && Notes.belongsTo(Case.getId())) {
                Stack.set(ArchiveRegistry.NotesComponent.get(), new FieldNotes(Lines, false, Optional.of(Case.getId())));
            }
        }
    }
}
