package net.rai.scp_unleashed.archive.investigation;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.ArchiveRules;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.archive.document.SpawnRule;
import net.rai.scp_unleashed.notice.Notices;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InvestigationTracker {
    public static final int TickInterval = 20;
    public static final long UnsurfacedCooldown = 12000L;
    public static final long FailedCooldown = 200L;
    private static final Logger Log = LogUtils.getLogger();

    private InvestigationTracker() {
    }

    public record Opened(List<Component> Notes, UUID CaseId) {
    }

    public static Opened open(ServerPlayer Reader, ResourceLocation Id, AnomalyDocument Document) {
        Investigations Data = Investigations.get(Reader.server);
        if (Document.Spawn() == SpawnRule.Once && !ArchiveRules.unlimited(Reader.server)) {
            Optional<Investigation> Existing = Data.find(Reader.getUUID(), Id);
            if (Existing.isPresent()) {
                return new Opened(Existing.get().getNotes(), Existing.get().getId());
            }
        }
        Investigation Case = new Investigation(Id, Reader.level().getGameTime());
        List<Component> Notes = AnomalyRealm.allows(Reader.level(), Id) ? Document.Condition().begin(Reader, Case) : RealmHold.hold(Case);
        Case.setNotes(Notes);
        Case.consumeDirty();
        Data.add(Reader.getUUID(), Case);
        Data.noteOpened(Reader.server);
        return new Opened(Notes, Case.getId());
    }

    public static void tick(ServerPlayer Reader) {
        if (!ArchiveRules.spawning(Reader.server) || !ReaderTargets.isSettled(Reader)) {
            return;
        }
        boolean Open = RealmHold.ready(Reader);
        Investigations Data = Investigations.get(Reader.server);
        List<Investigation> Cases = Data.of(Reader.getUUID());
        for (Investigation Case : Cases) {
            if (!Open && !AnomalyRealm.allows(Reader.level(), Case.getDocument())) {
                continue;
            }
            AnomalyDocument Document = AnomalyDocuments.server().get(Case.getDocument());
            long Now = Reader.serverLevel().getGameTime();
            if (Document == null || Reader.isRemoved() || Now < Case.getRecheck()) {
                continue;
            }
            if (RealmHold.resume(Reader, Case, Document)) {
                Data.setDirty();
            }
            Optional<StartCondition.Finding> Found;
            try {
                Found = Document.Condition().tick(Reader, Case);
            } catch (RuntimeException Problem) {
                Log.error("Investigation of {} failed to tick", Case.getDocument(), Problem);
                continue;
            }
            if (Case.consumeDirty()) {
                Data.setDirty();
            }
            if (Found.isEmpty()) {
                continue;
            }
            AnomalySpawners.Outcome Result = AnomalySpawners.discover(new AnomalySpawners.Discovery(Reader.serverLevel(), Found.get().Pos(), Reader, Case.getDocument(), Document,
                    Found.get().Manifested()));
            if (Result == AnomalySpawners.Outcome.Spawned) {
                Data.remove(Reader.getUUID(), Case);
                markResolved(Reader, Case.getDocument(), Case.getId());
                stampFormer(Reader.server, Data, Case);
                if (Found.get().Manifested() && Reader.isSleeping()) {
                    Reader.stopSleepInBed(true, true);
                }
                continue;
            }
            Case.deferUntil(Now + (Result == AnomalySpawners.Outcome.Unsurfaced ? UnsurfacedCooldown : FailedCooldown));
            Case.consumeDirty();
            Data.setDirty();
            if (Result == AnomalySpawners.Outcome.Unsurfaced) {
                Notices.caseInfo(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.unsurfaced"), true);
            }
        }
    }

    public static void reassign(MinecraftServer Server) {
        if (!ReaderTargets.anyTargetable(Server)) {
            return;
        }
        Investigations Data = Investigations.get(Server);
        long Now = Server.overworld().getGameTime();
        for (UUID Absent : Data.readers()) {
            if (Server.getPlayerList().getPlayer(Absent) != null) {
                continue;
            }
            for (Investigation Case : Data.of(Absent)) {
                AnomalyDocument Document = AnomalyDocuments.server().get(Case.getDocument());
                if (Document == null || Document.Condition().managesReaders()) {
                    continue;
                }
                ServerPlayer Heir = ReaderTargets.next(Server, Absent, Data::count, Candidate -> Data.find(Candidate.getUUID(), Case.getDocument()).isEmpty());
                if (Heir != null) {
                    handOff(Server, Data, Case, Absent, Heir, Now);
                }
            }
        }
    }

    private static void handOff(MinecraftServer Server, Investigations Data, Investigation Case, UUID From, ServerPlayer Heir, long Now) {
        Data.remove(From, Case);
        Case.addFormer(From);
        Case.dropFormer(Heir.getUUID());
        Case.deferUntil(Math.max(Case.getRecheck(), Now + ReaderTargets.JoinGrace));
        Case.consumeDirty();
        Data.add(Heir.getUUID(), Case);
        ReaderTargets.moved(Server, Case.getId(), From, Heir.getUUID());
    }

    public static void joined(ServerPlayer Reader) {
        ReaderTargets.joined(Reader);
        for (Investigations.Stamp Due : Investigations.get(Reader.server).takeStamps(Reader.getUUID())) {
            markResolved(Reader, Due.Document(), Due.CaseId());
        }
    }

    private static void stampFormer(MinecraftServer Server, Investigations Data, Investigation Case) {
        for (UUID Earlier : Case.getFormer()) {
            ServerPlayer Online = Server.getPlayerList().getPlayer(Earlier);
            if (Online != null) {
                markResolved(Online, Case.getDocument(), Case.getId());
            } else {
                Data.deferStamp(Earlier, Case.getDocument(), Case.getId());
            }
        }
    }

    public static void markResolved(ServerPlayer Reader, ResourceLocation Id, UUID CaseId) {
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Stack.is(ArchiveRegistry.OpenedDocument.get()) && Stamp != null && Stamp.Document().equals(Id) && Notes != null && !Notes.Resolved() && Notes.belongsTo(CaseId)) {
                Stack.set(ArchiveRegistry.NotesComponent.get(), Notes.resolve());
            }
        }
    }
}
