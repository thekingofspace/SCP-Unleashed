package net.rai.scp_unleashed.scp049;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

public record Scp049VillageCondition(int MaxDistance, int Accuracy, int Arrival, int FallbackMin, int FallbackMax) implements StartCondition {
    public static final String TypeName = "scp049_village";
    public static final String Locating = "locating";
    public static final String Village = "village";
    public static final String Fallback = "fallback";
    public static final int NearDistance = 160;
    public static final int SiteRetries = 8;
    public static final MapCodec<Scp049VillageCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(128, 12000).optionalFieldOf("max_distance", 2400).forGetter(Scp049VillageCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 32).forGetter(Scp049VillageCondition::Accuracy),
            Codec.intRange(16, 256).optionalFieldOf("arrival", 64).forGetter(Scp049VillageCondition::Arrival),
            Codec.intRange(64, 12000).optionalFieldOf("fallback_min", 600).forGetter(Scp049VillageCondition::FallbackMin),
            Codec.intRange(64, 12000).optionalFieldOf("fallback_max", 1500).forGetter(Scp049VillageCondition::FallbackMax)
    ).apply(Instance, Scp049VillageCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        CompoundTag State = Case.getState();
        State.putString("Dimension", LeadPlanner.dimensionOf(World));
        State.putInt("OriginX", Reader.getBlockX());
        State.putInt("OriginY", Reader.getBlockY());
        State.putInt("OriginZ", Reader.getBlockZ());
        State.putString("Phase", Locating);
        Case.markDirty();
        Scp049Locator.start(World, Case.getId(), Reader.blockPosition(), MaxDistance);
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp049.0"),
                Component.translatable("archive.scp_unleashed.notes.scp049.locating"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        CompoundTag State = Case.getState();
        ServerLevel World = Reader.serverLevel();
        if (!LeadPlanner.inDimension(Reader, State.getString("Dimension"))) {
            return Optional.empty();
        }
        String Phase = State.getString("Phase");
        if (Locating.equals(Phase) || Phase.isEmpty()) {
            locate(Reader, Case, World, State);
            return Optional.empty();
        }
        int X = State.getInt("X");
        int Z = State.getInt("Z");
        double Distance = LeadPlanner.horizontalDistance(Reader, X, Z);
        if (Distance <= Arrival) {
            BlockPos Column = new BlockPos(X, Reader.getBlockY(), Z);
            if (!World.isLoaded(Column)) {
                return Optional.empty();
            }
            BlockPos Ground = LeadPlanner.surface(World, X, Z, Reader.getBlockY());
            Optional<BlockPos> Site = Village.equals(Phase) ? Scp049Discovery.villageSite(World, Ground, Reader.getRandom()) : Optional.empty();
            if (Site.isEmpty()) {
                Site = Scp049Discovery.openSite(World, Ground, Reader.getRandom());
            }
            if (Site.isEmpty()) {
                int Tries = State.getInt("Tries") + 1;
                State.putInt("Tries", Tries);
                Case.markDirty();
                if (Tries >= SiteRetries) {
                    Site = Scp049Discovery.openSite(World, Reader.blockPosition().offset(6, 0, 6), Reader.getRandom());
                }
            }
            return Site.map(Pos -> new Finding(Pos, false));
        }
        if (Distance <= NearDistance && !State.getBoolean("Near")) {
            State.putBoolean("Near", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp049_near"), false);
            LeadPlanner.playTo(Reader, Scp049Registry.BreatheOut, LeadPlanner.towards(Reader, X + 0.5, Z + 0.5, 6.0), 0.5f, 0.9f);
        }
        return Optional.empty();
    }

    private void locate(ServerPlayer Reader, Investigation Case, ServerLevel World, CompoundTag State) {
        Optional<Optional<BlockPos>> Result = Scp049Locator.poll(Case.getId());
        if (Result.isEmpty()) {
            if (!Scp049Locator.isRunning(Case.getId())) {
                Scp049Locator.start(World, Case.getId(), origin(State), MaxDistance);
            }
            return;
        }
        BlockPos Origin = origin(State);
        List<Component> Lines = new ArrayList<>();
        if (Result.get().isPresent()) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.scp049.0"));
            BlockPos Found = Result.get().get();
            State.putString("Phase", Village);
            State.putInt("X", Found.getX());
            State.putInt("Z", Found.getZ());
            int ShownX = LeadPlanner.roughly(Found.getX(), Accuracy, Reader.getRandom());
            int ShownZ = LeadPlanner.roughly(Found.getZ(), Accuracy, Reader.getRandom());
            Lines.add(Component.translatable("archive.scp_unleashed.notes.scp049.village", ShownX, ShownZ));
            Lines.add(Component.translatable("archive.scp_unleashed.notes.scp049.missing"));
        } else {
            BlockPos Target = LeadPlanner.pickTarget(World, Origin, FallbackMin, FallbackMax, Reader.getRandom());
            State.putString("Phase", Fallback);
            State.putInt("X", Target.getX());
            State.putInt("Z", Target.getZ());
            int ShownX = LeadPlanner.roughly(Target.getX(), Accuracy, Reader.getRandom());
            int ShownZ = LeadPlanner.roughly(Target.getZ(), Accuracy, Reader.getRandom());
            Lines.add(Component.translatable("archive.scp_unleashed.notes.scp049.no_village", MaxDistance));
            Lines.add(Component.translatable("archive.scp_unleashed.notes.scp049.fallback", ShownX, ShownZ));
        }
        if (!World.dimension().equals(Level.OVERWORLD)) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.dimension", LeadPlanner.dimensionName(State.getString("Dimension"))));
        } else {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.scp049.advice"));
        }
        Case.setNotes(Lines);
        Case.markDirty();
        rewriteNotes(Reader, Case, Lines);
        Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp049_traced"), false);
    }

    private static BlockPos origin(CompoundTag State) {
        return new BlockPos(State.getInt("OriginX"), State.getInt("OriginY"), State.getInt("OriginZ"));
    }

    private static void rewriteNotes(ServerPlayer Reader, Investigation Case, List<Component> Lines) {
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Stack.is(ArchiveRegistry.OpenedDocument.get()) && Stamp != null && Stamp.Document().equals(Scp049Registry.DocumentId) && Notes != null && !Notes.Resolved()
                    && Notes.belongsTo(Case.getId())) {
                Stack.set(ArchiveRegistry.NotesComponent.get(), new FieldNotes(Lines, false, Optional.of(Case.getId())));
            }
        }
    }
}
