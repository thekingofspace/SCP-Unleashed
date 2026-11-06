package net.rai.scp_unleashed.scp143;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record Scp143GroveCondition(int MinDistance, int MaxDistance, int Arrival) implements StartCondition {
    public static final String TypeName = "scp143_grove";
    public static final int NearDistance = 160;
    public static final MapCodec<Scp143GroveCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(64, 4000).optionalFieldOf("min_distance", 320).forGetter(Scp143GroveCondition::MinDistance),
            Codec.intRange(64, 4000).optionalFieldOf("max_distance", 640).forGetter(Scp143GroveCondition::MaxDistance),
            Codec.intRange(8, 128).optionalFieldOf("arrival", 40).forGetter(Scp143GroveCondition::Arrival)
    ).apply(Instance, Scp143GroveCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    public static BlockPos ensureSite(MinecraftServer Server, BlockPos Origin, int MinDistance, int MaxDistance, ServerPlayer Reader) {
        Scp143World Data = Scp143World.get(Server);
        BlockPos Site = Data.site();
        if (Site == null) {
            Site = Scp143Sites.plan(Server.overworld(), Origin, MinDistance, MaxDistance, Reader.getRandom());
            Data.plan(Site);
        }
        return Site;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        MinecraftServer Server = Reader.server;
        ServerLevel Overworld = Server.overworld();
        boolean Here = Reader.serverLevel().dimension() == Level.OVERWORLD;
        BlockPos Site = ensureSite(Server, Here ? Reader.blockPosition() : Overworld.getSharedSpawnPos(), MinDistance, MaxDistance, Reader);
        CompoundTag State = Case.getState();
        State.putBoolean("Away", !Here);
        store(Case, Site);
        return notes(Site, !Here, false);
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        MinecraftServer Server = Reader.server;
        Scp143World Data = Scp143World.get(Server);
        CompoundTag State = Case.getState();
        BlockPos Site = Data.site();
        if (Site == null) {
            Site = ensureSite(Server, Server.overworld().getSharedSpawnPos(), MinDistance, MaxDistance, Reader);
        }
        if (!matches(State, Site)) {
            boolean Moved = State.contains("X");
            store(Case, Site);
            List<Component> Lines = notes(Site, State.getBoolean("Away"), Moved);
            Case.setNotes(Lines);
            rewrite(Reader, Case, Lines);
            if (Moved) {
                Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp143_moved"), true);
            }
        }
        if (State.contains("Scheduled") || State.contains("Rescheduled")) {
            State.remove("Scheduled");
            State.remove("Rescheduled");
            Case.markDirty();
        }
        ServerLevel World = Reader.serverLevel();
        if (World.dimension() != Level.OVERWORLD || Reader.isSpectator()) {
            return Optional.empty();
        }
        double Distance = LeadPlanner.horizontalDistance(Reader, Site.getX() + 1, Site.getZ() + 1);
        if (Distance <= NearDistance && !State.getBoolean("Near")) {
            State.putBoolean("Near", true);
            Case.markDirty();
            Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp143_near"), false);
            LeadPlanner.playTo(Reader, Scp143Registry.Chime, LeadPlanner.towards(Reader, Site.getX() + 1.0, Site.getZ() + 1.0, 6.0), 0.5f, 0.85f);
        }
        if (Data.isBuilt() && Distance <= Arrival) {
            return Optional.of(new Finding(Site, false));
        }
        return Optional.empty();
    }

    private static boolean matches(CompoundTag State, BlockPos Site) {
        return State.contains("X") && State.getInt("X") == Site.getX() && State.getInt("Y") == Site.getY() && State.getInt("Z") == Site.getZ();
    }

    private static void store(Investigation Case, BlockPos Site) {
        CompoundTag State = Case.getState();
        State.putInt("X", Site.getX());
        State.putInt("Y", Site.getY());
        State.putInt("Z", Site.getZ());
        Case.markDirty();
    }

    public static List<Component> notes(BlockPos Site, boolean Away, boolean Moved) {
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable("archive.scp_unleashed.notes.scp143.site", Site.getX(), Site.getY(), Site.getZ()));
        if (Moved) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.scp143.moved"));
        }
        if (Away) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.dimension", LeadPlanner.dimensionName(Level.OVERWORLD.location().toString())));
        }
        if (!Moved && !Away) {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.scp143.0"));
        }
        Lines.add(Component.translatable("archive.scp_unleashed.notes.scp143.1"));
        Lines.add(Component.translatable("archive.scp_unleashed.notes.scp143.2"));
        return Lines;
    }

    private static void rewrite(ServerPlayer Reader, Investigation Case, List<Component> Lines) {
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Stack.is(ArchiveRegistry.OpenedDocument.get()) && Stamp != null && Stamp.Document().equals(Scp143Registry.DocumentId) && Notes != null && !Notes.Resolved()
                    && Notes.belongsTo(Case.getId())) {
                Stack.set(ArchiveRegistry.NotesComponent.get(), new FieldNotes(Lines, false, Optional.of(Case.getId())));
            }
        }
    }

    public static void retireContacts(MinecraftServer Server) {
        Map<UUID, List<UUID>> Withdrawn = StrangerEncounters.get(Server).withdraw(Scp143Registry.DocumentId);
        if (Withdrawn.isEmpty()) {
            return;
        }
        Investigations Data = Investigations.get(Server);
        for (UUID Owner : Data.readers()) {
            for (Investigation Case : Data.of(Owner)) {
                List<UUID> Earlier = Withdrawn.get(Case.getId());
                if (Earlier == null) {
                    continue;
                }
                for (UUID Previous : Earlier) {
                    if (!Previous.equals(Owner)) {
                        Case.addFormer(Previous);
                    }
                }
                Case.consumeDirty();
                Data.setDirty();
            }
        }
    }

    public static void refreshAll(MinecraftServer Server) {
        BlockPos Site = Scp143World.get(Server).site();
        if (Site == null) {
            return;
        }
        Investigations Data = Investigations.get(Server);
        for (UUID Reader : Data.readers()) {
            for (Investigation Case : Data.of(Reader)) {
                if (!Case.getDocument().equals(Scp143Registry.DocumentId) || matches(Case.getState(), Site)) {
                    continue;
                }
                boolean Moved = Case.getState().contains("X");
                store(Case, Site);
                List<Component> Lines = notes(Site, Case.getState().getBoolean("Away"), Moved);
                Case.setNotes(Lines);
                Case.consumeDirty();
                Data.setDirty();
                ServerPlayer Online = Server.getPlayerList().getPlayer(Reader);
                if (Online != null) {
                    rewrite(Online, Case, Lines);
                    if (Moved) {
                        Notices.lead(Online, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.scp143_moved"), true);
                    }
                }
            }
        }
    }
}
