package net.rai.scp_unleashed.archive.database;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.archive.AnomalyDocumentItem;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class DatabaseCopyItem extends AnomalyDocumentItem {
    public DatabaseCopyItem(Properties Properties) {
        super(Properties);
    }

    public static ItemStack create(DatabaseRecord Entry) {
        ItemStack Stack = new ItemStack(ArchiveRegistry.DatabaseCopy.get());
        Stack.set(ArchiveRegistry.StampComponent.get(), new DocumentStamp(Entry.Document(), Entry.Classification()));
        Stack.set(ArchiveRegistry.NotesComponent.get(), DatabaseRecord.snapshot(Entry.Notes()));
        return Stack;
    }

    @Override
    public Component getName(ItemStack Stack) {
        AnomalyDocument Document = lookup(Stack);
        if (Document == null) {
            return super.getName(Stack);
        }
        return Component.translatable("item.scp_unleashed.archive_database_copy.named", Document.designation());
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        if (Stamp == null) {
            Lines.add(Component.translatable("item.scp_unleashed.anomaly_document.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        AnomalyDocument Document = lookup(Stack);
        DocumentClass Classification = Stamp.Classification();
        if (Document != null) {
            Lines.add(Document.title().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
        } else {
            Lines.add(Component.translatable("archive.scp_unleashed.notes.corrupted").withStyle(ChatFormatting.DARK_RED));
        }
        Lines.add(Component.translatable("item.scp_unleashed.sealed_document.class", Classification.getName().copy()
                .withStyle(Look -> Look.withColor(Classification.getColour() & 0xFFFFFF).withBold(true))).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("item.scp_unleashed.archive_database_copy.status").withStyle(ChatFormatting.AQUA));
        Lines.add(Component.translatable("item.scp_unleashed.anomaly_document.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.archive_database_copy.inert").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.archive_database_copy.return").withStyle(ChatFormatting.GOLD));
            FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
            if (Notes != null) {
                for (Component Line : Notes.readableLines()) {
                    Lines.add(Line.copy().withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}
