package net.rai.scp_unleashed.archive.investigation;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.notice.Notices;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public final class RealmHold {
    public static final String HeldKey = "RealmHeld";
    public static final String ColdKey = "scp_unleashed_trail_cold";
    private static final Logger Log = LogUtils.getLogger();

    private RealmHold() {
    }

    public static List<Component> hold(Investigation Case) {
        Case.getState().putBoolean(HeldKey, true);
        Case.markDirty();
        return List.of(Component.translatable("archive.scp_unleashed.notes.realm_held"));
    }

    public static boolean isHeld(Investigation Case) {
        return Case.getState().getBoolean(HeldKey);
    }

    public static boolean ready(ServerPlayer Reader) {
        CompoundTag Data = Reader.getPersistentData();
        if (AnomalyRealm.allows(Reader.level())) {
            Data.remove(ColdKey);
            return true;
        }
        if (!Data.getBoolean(ColdKey) && Investigations.get(Reader.server).of(Reader.getUUID()).stream()
                .anyMatch(Case -> !AnomalyRealm.allows(Reader.level(), Case.getDocument()))) {
            Data.putBoolean(ColdKey, true);
            Notices.lead(Reader, null, Component.translatable("archive.scp_unleashed.lead.realm_cold"), false);
        }
        return false;
    }

    public static boolean resume(ServerPlayer Reader, Investigation Case, AnomalyDocument Document) {
        if (!isHeld(Case)) {
            return false;
        }
        Case.getState().remove(HeldKey);
        Case.markDirty();
        List<Component> Lines;
        try {
            Lines = Document.Condition().begin(Reader, Case);
        } catch (RuntimeException Problem) {
            Log.error("Investigation of {} failed to resume", Case.getDocument(), Problem);
            return true;
        }
        Case.setNotes(Lines);
        rewriteNotes(Reader, Case, Lines);
        Notices.lead(Reader, Case.getDocument(), Component.translatable("archive.scp_unleashed.lead.realm_resumed"), true);
        return true;
    }

    private static void rewriteNotes(ServerPlayer Reader, Investigation Case, List<Component> Lines) {
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Stack.is(ArchiveRegistry.OpenedDocument.get()) && Stamp != null && Stamp.Document().equals(Case.getDocument()) && Notes != null && !Notes.Resolved()
                    && Notes.belongsTo(Case.getId())) {
                Stack.set(ArchiveRegistry.NotesComponent.get(), new FieldNotes(Lines, false, Optional.of(Case.getId())));
            }
        }
    }
}
