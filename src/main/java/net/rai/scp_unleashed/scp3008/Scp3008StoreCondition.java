package net.rai.scp_unleashed.scp3008;

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
import java.util.UUID;

public record Scp3008StoreCondition(int MinDistance, int MaxDistance, int Accuracy, int Arrival) implements StartCondition {
    public static final String TypeName = "scp3008_store";
    public static final int PlanAttempts = 48;
    public static final int CheckTicks = 20;
    public static final int SiteRadius = 16;
    public static final int SpreadStep = 16;
    public static final int MaxSpread = 64;
    public static final int StrictTries = 4;
    public static final int LenientTries = 10;
    public static final int NearDistance = 192;
    public static final MapCodec<Scp3008StoreCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(16, 6000).optionalFieldOf("min_distance", 240).forGetter(Scp3008StoreCondition::MinDistance),
            Codec.intRange(16, 6000).optionalFieldOf("max_distance", 520).forGetter(Scp3008StoreCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 32).forGetter(Scp3008StoreCondition::Accuracy),
            Codec.intRange(8, 256).optionalFieldOf("arrival", 64).forGetter(Scp3008StoreCondition::Arrival)
    ).apply(Instance, Scp3008StoreCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Scp3008State Data = Scp3008State.get(Reader.server);
        if (Data.hasStore()) {
            return knownNotes(Data.door());
        }
        if (Reader.serverLevel().dimension() != Level.OVERWORLD) {
            return List.of(
                    Component.translatable("archive.scp_unleashed.notes.scp3008.0"),
                    Component.translatable("archive.scp_unleashed.notes.scp3008.away"));
        }
        BlockPos Target = plan(Reader.serverLevel(), Reader.blockPosition(), Reader.getRandom());
        store(Case, Target);
        return notes(Reader, Target);
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        Scp3008State Data = Scp3008State.get(Reader.server);
        BlockPos Door = Data.door();
        if (Door != null) {
            return World.dimension() == Data.doorDimension() ? Optional.of(new Finding(Door, false)) : Optional.empty();
        }
        if (World.dimension() != Level.OVERWORLD || Reader.isSpectator()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        if (!State.contains("X")) {
            BlockPos Target = plan(World, Reader.blockPosition(), Reader.getRandom());
            store(Case, Target);
            List<Component> Lines = notes(Reader, Target);
            Case.setNotes(Lines);
            Case.markDirty();
            rewriteNotes(Reader, Case.getId(), Lines);
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp3008_traced"), true);
            return Optional.empty();
        }
        int X = State.getInt("X");
        int Z = State.getInt("Z");
        double Distance = LeadPlanner.horizontalDistance(Reader, X, Z);
        if (Distance <= NearDistance && !State.getBoolean("Near")) {
            State.putBoolean("Near", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp3008_near"), false);
        }
        long Now = World.getGameTime();
        if (Now < State.getLong("Next")) {
            return Optional.empty();
        }
        State.putLong("Next", Now + CheckTicks);
        if (!World.isLoaded(new BlockPos(X, World.getSeaLevel(), Z)) || Distance > Arrival + MaxSpread + Scp3008Storefront.HalfWidth * 2) {
            return Optional.empty();
        }
        int Tries = State.getInt("Tries");
        int Spread = Math.min(MaxSpread, SiteRadius + Tries * SpreadStep);
        int Mode = Tries < StrictTries ? Scp3008Storefront.Strict : Tries < LenientTries ? Scp3008Storefront.Lenient : Scp3008Storefront.Forced;
        Optional<BlockPos> Site = Scp3008Storefront.findSite(World, Reader, new Vec3(X + 0.5, Reader.getY(), Z + 0.5), 0, Spread, Mode, Reader.getRandom());
        if (Site.isPresent()) {
            return Site.map(Pos -> new Finding(Pos, false));
        }
        State.putInt("Tries", Tries + 1);
        Case.markDirty();
        if (Tries + 1 >= LenientTries && Distance <= Arrival) {
            return Scp3008Storefront.findSite(World, Reader, Reader.position(), 48, 80, Scp3008Storefront.Forced, Reader.getRandom()).map(Pos -> new Finding(Pos, false));
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
        return !Found.is(BiomeTags.IS_OCEAN) && !Found.is(BiomeTags.IS_DEEP_OCEAN) && !Found.is(BiomeTags.IS_RIVER) && !Found.is(BiomeTags.IS_BEACH)
                && !Found.is(BiomeTags.IS_MOUNTAIN);
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
        Lines.add(Component.translatable("archive.scp_unleashed.notes.scp3008.1"));
        return Lines;
    }

    private static List<Component> knownNotes(BlockPos Door) {
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp3008.known", Door.getX(), Door.getZ()),
                Component.translatable("archive.scp_unleashed.notes.scp3008.1"));
    }

    public static void rewriteNotes(ServerPlayer Reader, UUID CaseId, List<Component> Lines) {
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Stack.is(ArchiveRegistry.OpenedDocument.get()) && Stamp != null && Stamp.Document().equals(Scp3008Registry.DocumentId) && Notes != null && !Notes.Resolved()
                    && Notes.belongsTo(CaseId)) {
                Stack.set(ArchiveRegistry.NotesComponent.get(), new FieldNotes(Lines, false, Optional.of(CaseId)));
            }
        }
    }
}
