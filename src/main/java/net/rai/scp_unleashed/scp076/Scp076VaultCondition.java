package net.rai.scp_unleashed.scp076;

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
import net.rai.scp_unleashed.archive.ArchiveRules;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.notice.Notices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scp076VaultCondition(int MinDistance, int MaxDistance, int Accuracy, int Arrival) implements StartCondition {
    public static final String TypeName = "scp076_vault";
    public static final int NearDistance = 160;
    public static final int TargetAttempts = 24;
    public static final int SearchRadius = 40;
    public static final int WideRadius = 80;
    public static final int WideAfter = 3;
    public static final int MoveAfter = 8;
    public static final int MoveMin = 200;
    public static final int MoveMax = 400;
    public static final MapCodec<Scp076VaultCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(64, 12000).optionalFieldOf("min_distance", 400).forGetter(Scp076VaultCondition::MinDistance),
            Codec.intRange(64, 12000).optionalFieldOf("max_distance", 1200).forGetter(Scp076VaultCondition::MaxDistance),
            Codec.intRange(0, 256).optionalFieldOf("accuracy", 32).forGetter(Scp076VaultCondition::Accuracy),
            Codec.intRange(16, 160).optionalFieldOf("arrival", 48).forGetter(Scp076VaultCondition::Arrival)
    ).apply(Instance, Scp076VaultCondition::new));

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
        Optional<Scp076Vaults.Vault> Existing = ArchiveRules.repeatable(Reader.server) ? Optional.empty() : Scp076Vaults.get(Reader.server).first(Level.OVERWORLD);
        BlockPos Target;
        if (Existing.isPresent()) {
            Target = Existing.get().Centre();
            State.putBoolean("Known", true);
        } else {
            Target = pick(World, Origin, Reader);
        }
        State.putString("Dimension", LeadPlanner.dimensionOf(World));
        State.putInt("X", Target.getX());
        State.putInt("Z", Target.getZ());
        Case.markDirty();
        return notes(Reader, Target, Here, false);
    }

    private BlockPos pick(ServerLevel World, BlockPos Origin, ServerPlayer Reader) {
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < TargetAttempts; Attempt++) {
            BlockPos Candidate = LeadPlanner.pickTarget(World, Origin, MinDistance, MaxDistance, Reader.getRandom());
            if (Fallback == null) {
                Fallback = Candidate;
            }
            if (Scp076Vault.acceptableBiome(World.getBiome(Candidate.atY(World.getSeaLevel() + 4))) && !Scp076Sites.villageNear(World, Candidate, 200)) {
                return Candidate;
            }
        }
        return Fallback;
    }

    private List<Component> notes(ServerPlayer Reader, BlockPos Target, boolean Here, boolean Moved) {
        int ShownX = LeadPlanner.roughly(Target.getX(), Accuracy, Reader.getRandom());
        int ShownZ = LeadPlanner.roughly(Target.getZ(), Accuracy, Reader.getRandom());
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable(Moved ? "archive.scp_unleashed.notes.scp076.moved" : "archive.scp_unleashed.notes.scp076.0"));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site", ShownX, ShownZ));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.site.radius", Accuracy + Arrival));
        if (!Here) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.dimension", LeadPlanner.dimensionName(LeadPlanner.dimensionOf(Reader.server.overworld()))));
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
            BlockPos Column = new BlockPos(X, Reader.getBlockY(), Z);
            if (!World.isLoaded(Column)) {
                return Optional.empty();
            }
            Optional<Scp076Vaults.Vault> Known = Scp076Vaults.get(Reader.server).nearest(World.dimension(), Column, 128);
            if (Known.isPresent()) {
                return Optional.of(new Finding(Known.get().Centre(), false));
            }
            int Tries = State.getInt("Tries");
            Optional<BlockPos> Site = Scp076Vault.findSite(World, Column, Tries >= WideAfter ? WideRadius : SearchRadius,
                    Tries >= WideAfter ? Scp076Vault.LooseSlope : Scp076Vault.StrictSlope, Reader.getRandom());
            if (Site.isPresent()) {
                return Site.map(Pos -> new Finding(Pos, false));
            }
            Tries++;
            State.putInt("Tries", Tries);
            if (Tries >= MoveAfter) {
                BlockPos Next = LeadPlanner.pickTarget(World, Column, MoveMin, MoveMax, Reader.getRandom());
                State.putInt("X", Next.getX());
                State.putInt("Z", Next.getZ());
                State.putInt("Tries", 0);
                State.putBoolean("Near", false);
                List<Component> Lines = notes(Reader, Next, true, true);
                Case.setNotes(Lines);
                rewriteNotes(Reader, Case, Lines);
                Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp076_moved"), true);
            }
            Case.markDirty();
            return Optional.empty();
        }
        if (Distance <= NearDistance && !State.getBoolean("Near")) {
            State.putBoolean("Near", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp076_near"), false);
            LeadPlanner.playTo(Reader, Scp076Registry.LockRattle, LeadPlanner.towards(Reader, X + 0.5, Z + 0.5, 6.0), 0.35f, 0.6f);
        }
        return Optional.empty();
    }

    private static void rewriteNotes(ServerPlayer Reader, Investigation Case, List<Component> Lines) {
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Stack.is(ArchiveRegistry.OpenedDocument.get()) && Stamp != null && Stamp.Document().equals(Scp076Registry.DocumentId) && Notes != null && !Notes.Resolved()
                    && Notes.belongsTo(Case.getId())) {
                Stack.set(ArchiveRegistry.NotesComponent.get(), new FieldNotes(Lines, false, Optional.of(Case.getId())));
            }
        }
    }
}
