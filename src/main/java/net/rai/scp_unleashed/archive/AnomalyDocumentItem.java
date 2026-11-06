package net.rai.scp_unleashed.archive;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.archive.client.ArchiveClientHooks;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.discord.DiscordNotices;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AnomalyDocumentItem extends Item {
    public AnomalyDocumentItem(Properties Properties) {
        super(Properties);
    }

    public static ItemStack create(ResourceLocation Id, DocumentClass Classification, FieldNotes Notes) {
        ItemStack Stack = new ItemStack(ArchiveRegistry.OpenedDocument.get());
        Stack.set(ArchiveRegistry.StampComponent.get(), new DocumentStamp(Id, Classification));
        Stack.set(ArchiveRegistry.NotesComponent.get(), Notes);
        return Stack;
    }

    @Nullable
    public static AnomalyDocument lookup(ItemStack Stack) {
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        if (Stamp == null) {
            return null;
        }
        return AnomalyDocuments.lookup(Stamp.Document());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (Stack.get(ArchiveRegistry.StampComponent.get()) == null) {
            return InteractionResultHolder.fail(Stack);
        }
        if (World.isClientSide) {
            ArchiveClientHooks.openDossier(Hand, false);
        } else if (User instanceof ServerPlayer Reader) {
            DiscordNotices.documentRead(Reader, Stack);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public Component getName(ItemStack Stack) {
        AnomalyDocument Document = lookup(Stack);
        if (Document == null) {
            return super.getName(Stack);
        }
        return Component.translatable("item.scp_unleashed.anomaly_document.named", Document.designation());
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
        FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
        boolean Resolved = Notes != null && Notes.Resolved();
        if (!Resolved) {
            Lines.add(Component.translatable("item.scp_unleashed.anomaly_document.open").withStyle(ChatFormatting.YELLOW));
        }
        Lines.add(Component.translatable("item.scp_unleashed.anomaly_document.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            if (Document != null) {
                Lines.add(Component.translatable("item.scp_unleashed.anomaly_document.tier", Document.dangerNumeral()).withStyle(ChatFormatting.GOLD));
                Lines.add(Component.translatable("item.scp_unleashed.anomaly_document.spawn", Document.Spawn().getName()).withStyle(ChatFormatting.GOLD));
            }
            Lines.add(Component.translatable("item.scp_unleashed.anomaly_document.database").withStyle(ChatFormatting.DARK_AQUA));
            Lines.add(Component.translatable("item.scp_unleashed.anomaly_document.database_shred").withStyle(ChatFormatting.DARK_AQUA));
            if (Notes != null) {
                for (Component Line : Notes.readableLines()) {
                    Lines.add(Line.copy().withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}
