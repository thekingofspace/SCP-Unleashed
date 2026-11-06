package net.rai.scp_unleashed.scp4845;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.space.SpaceRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Scp4845SiteCondition(int MinDistance, int MaxDistance, int Accuracy, int Arrival) implements StartCondition {
    public static final String TypeName = "scp4845_moon";
    public static final int PlanAttempts = 32;
    public static final int CheckTicks = 20;
    public static final int SiteRadius = 16;
    public static final int SpreadStep = 16;
    public static final int MaxSpread = 64;
    public static final int StrictTries = 4;
    public static final int LenientTries = 10;
    public static final int NearDistance = 192;
    public static final int FoundDistance = 160;
    public static final MapCodec<Scp4845SiteCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(16, 6000).optionalFieldOf("min_distance", 240).forGetter(Scp4845SiteCondition::MinDistance),
            Codec.intRange(16, 6000).optionalFieldOf("max_distance", 520).forGetter(Scp4845SiteCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 32).forGetter(Scp4845SiteCondition::Accuracy),
            Codec.intRange(8, 256).optionalFieldOf("arrival", 64).forGetter(Scp4845SiteCondition::Arrival)
    ).apply(Instance, Scp4845SiteCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Scp4845Site Placed = Scp4845Restaurant.current(Reader.server);
        if (Placed != null) {
            return knownNotes(Placed);
        }
        Scp4845State Data = Scp4845State.get(Reader.server);
        BlockPos Target = Data.planned();
        if (Target == null) {
            Target = plan(Reader, Reader.getRandom());
            Data.plan(Target);
        }
        store(Case, Target);
        return notes(Reader, Target);
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        Scp4845Site Placed = Scp4845Restaurant.current(Reader.server);
        if (Placed != null) {
            if (!Placed.in(World) || LeadPlanner.horizontalDistance(Reader, Placed.Centre.getX(), Placed.Centre.getZ()) > FoundDistance) {
                return Optional.empty();
            }
            return Optional.of(new Finding(Placed.Entrance.getFirst(), false));
        }
        if (!Scp4845Restaurant.isMoon(World) || Reader.isSpectator()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        if (!State.contains("X")) {
            Scp4845State Data = Scp4845State.get(Reader.server);
            BlockPos Target = Data.planned();
            if (Target == null) {
                Target = plan(Reader, Reader.getRandom());
                Data.plan(Target);
            }
            store(Case, Target);
            List<Component> Lines = notes(Reader, Target);
            Case.setNotes(Lines);
            rewriteNotes(Reader, Case.getId(), Lines);
        }
        if (!State.getBoolean("Landed")) {
            State.putBoolean("Landed", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp4845_landed"), true);
        }
        int X = State.getInt("X");
        int Z = State.getInt("Z");
        double Distance = LeadPlanner.horizontalDistance(Reader, X, Z);
        if (Distance <= NearDistance && !State.getBoolean("Near")) {
            State.putBoolean("Near", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp4845_near"), false);
        }
        long Now = World.getGameTime();
        if (Now < State.getLong("Next")) {
            return Optional.empty();
        }
        State.putLong("Next", Now + CheckTicks);
        if (!World.isLoaded(new BlockPos(X, World.getSeaLevel(), Z)) || Distance > Arrival + MaxSpread + Scp4845Restaurant.HalfSpan * 2) {
            return Optional.empty();
        }
        int Tries = State.getInt("Tries");
        int Spread = Math.min(MaxSpread, SiteRadius + Tries * SpreadStep);
        int Mode = Tries < StrictTries ? Scp4845Restaurant.Strict : Tries < LenientTries ? Scp4845Restaurant.Lenient : Scp4845Restaurant.Forced;
        Optional<BlockPos> Site = Scp4845Restaurant.findSite(World, Reader, new Vec3(X + 0.5, Reader.getY(), Z + 0.5), 0, Spread, Mode, Reader.getRandom());
        if (Site.isPresent()) {
            return Site.map(Pos -> new Finding(Pos, false));
        }
        State.putInt("Tries", Tries + 1);
        Case.markDirty();
        if (Tries + 1 >= LenientTries && Distance <= Arrival) {
            return Scp4845Restaurant.findSite(World, Reader, Reader.position(), 48, 80, Scp4845Restaurant.Forced, Reader.getRandom()).map(Pos -> new Finding(Pos, false));
        }
        return Optional.empty();
    }

    private BlockPos plan(ServerPlayer Reader, RandomSource Random) {
        ServerLevel Moon = Reader.server.getLevel(Scp4845Restaurant.moonKey());
        BlockPos Origin = Reader.blockPosition();
        if (Moon == null) {
            return LeadPlanner.pickTarget(Reader.serverLevel(), Origin, MinDistance, MaxDistance, Random);
        }
        int Loaded = Scp4845Restaurant.isMoon(Reader.level()) ? (Reader.server.getPlayerList().getViewDistance() + 2) * 16 : 0;
        int Low = Math.max(MinDistance, Loaded);
        int Span = Math.max(SiteRadius, MaxDistance - MinDistance);
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < PlanAttempts; Attempt++) {
            int Reach = Low + Attempt / 16 * 32;
            BlockPos Target = LeadPlanner.pickTarget(Moon, Origin, Reach, Reach + Span, Random);
            if (Loaded > 0 && Moon.getChunkSource().hasChunk(Target.getX() >> 4, Target.getZ() >> 4)) {
                if (Fallback == null) {
                    Fallback = Target;
                }
                continue;
            }
            return Target;
        }
        return Fallback != null ? Fallback : LeadPlanner.pickTarget(Moon, Origin, Low, Low + Span, Random);
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
        Lines.add(Component.translatable("archive.scp_unleashed.notes.dimension", LeadPlanner.dimensionName(SpaceRegistry.MoonLevel.location().toString())));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site", LeadPlanner.roughly(Target.getX(), Accuracy, Reader.getRandom()),
                LeadPlanner.roughly(Target.getZ(), Accuracy, Reader.getRandom())));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site.radius", Accuracy + Arrival));
        return Lines;
    }

    private static List<Component> knownNotes(Scp4845Site Placed) {
        BlockPos Door = Placed.Entrance.getFirst();
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp4845.known", Door.getX(), Door.getZ()),
                Component.translatable("archive.scp_unleashed.notes.dimension", LeadPlanner.dimensionName(Placed.Dimension.location().toString())));
    }

    public static void rewriteNotes(ServerPlayer Reader, UUID CaseId, List<Component> Lines) {
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Stack.is(ArchiveRegistry.OpenedDocument.get()) && Stamp != null && Stamp.Document().equals(Scp4845Registry.DocumentId) && Notes != null && !Notes.Resolved()
                    && Notes.belongsTo(CaseId)) {
                Stack.set(ArchiveRegistry.NotesComponent.get(), new FieldNotes(Lines, false, Optional.of(CaseId)));
            }
        }
    }
}
