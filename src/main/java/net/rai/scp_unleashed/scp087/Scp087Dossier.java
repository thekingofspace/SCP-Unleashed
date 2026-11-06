package net.rai.scp_unleashed.scp087;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class Scp087Dossier {
    public static final int InertLines = 3;

    private Scp087Dossier() {
    }

    public static List<Component> inertNotes() {
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp087.inert.0"),
                Component.translatable("archive.scp_unleashed.notes.scp087.inert.1"),
                Component.translatable("archive.scp_unleashed.notes.scp087.inert.2"));
    }

    private static boolean ours(ItemStack Stack) {
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        return Stamp != null && Stamp.Document().equals(Scp087Registry.DocumentId);
    }

    public static void refresh(ServerPlayer Reader) {
        if (!Scp087World.get(Reader.server).inert()) {
            return;
        }
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (!ours(Stack)) {
                continue;
            }
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            if (Stamp != null && Stamp.Classification() != DocumentClass.Neutralized) {
                Stack.set(ArchiveRegistry.StampComponent.get(), new DocumentStamp(Stamp.Document(), DocumentClass.Neutralized));
            }
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Stack.is(ArchiveRegistry.OpenedDocument.get()) && Notes != null && !Notes.Lines().equals(inertNotes())) {
                Stack.set(ArchiveRegistry.NotesComponent.get(), new FieldNotes(inertNotes(), true, Notes.CaseId()));
            }
        }
    }

    public static void restore(ServerPlayer Reader) {
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (!ours(Stack)) {
                continue;
            }
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            if (Stamp != null && Stamp.Classification() == DocumentClass.Neutralized) {
                Stack.set(ArchiveRegistry.StampComponent.get(), new DocumentStamp(Stamp.Document(), DocumentClass.Euclid));
            }
        }
    }

    public static void rewriteNotes(ServerPlayer Reader, UUID CaseId, List<Component> Lines) {
        Inventory Pockets = Reader.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Stack.is(ArchiveRegistry.OpenedDocument.get()) && ours(Stack) && Notes != null && !Notes.Resolved() && Notes.belongsTo(CaseId)) {
                Stack.set(ArchiveRegistry.NotesComponent.get(), new FieldNotes(Lines, false, Optional.of(CaseId)));
            }
        }
    }
}
