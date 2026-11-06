package net.rai.scp_unleashed.archive;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.archive.investigation.InvestigationTracker;
import net.rai.scp_unleashed.archive.network.OpenDossierPayload;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.discord.DiscordNotices;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class SealedDocumentItem extends Item {
    private static BooleanSupplier RevealCheck = () -> false;

    public SealedDocumentItem(Properties Properties) {
        super(Properties);
    }

    public static void setRevealCheck(BooleanSupplier Check) {
        RevealCheck = Check;
    }

    public static ItemStack create(ResourceLocation Id, DocumentClass Classification) {
        ItemStack Stack = new ItemStack(ArchiveRegistry.SealedDocument.get());
        Stack.set(ArchiveRegistry.StampComponent.get(), new DocumentStamp(Id, Classification));
        return Stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        if (Stamp == null) {
            return InteractionResultHolder.fail(Stack);
        }
        if (World.isClientSide || !(User instanceof ServerPlayer Reader)) {
            return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
        }
        AnomalyDocument Document = AnomalyDocuments.server().get(Stamp.Document());
        FieldNotes Notes;
        if (Document == null) {
            Notes = new FieldNotes(List.of(Component.translatable("archive.scp_unleashed.notes.corrupted")), false, Optional.empty());
        } else if (!ArchiveRules.spawning(Reader.server)) {
            Notes = new FieldNotes(List.of(Component.translatable("archive.scp_unleashed.notes.reference")), false, Optional.empty());
        } else {
            InvestigationTracker.Opened Case = InvestigationTracker.open(Reader, Stamp.Document(), Document);
            Notes = new FieldNotes(Case.Notes(), false, Optional.of(Case.CaseId()));
        }
        ItemStack Unsealed = new ItemStack(ArchiveRegistry.OpenedDocument.get());
        Unsealed.set(ArchiveRegistry.StampComponent.get(), Stamp);
        Unsealed.set(ArchiveRegistry.NotesComponent.get(), Notes);
        World.playSound(User, User.getX(), User.getY(), User.getZ(), ArchiveRegistry.Seal.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        User.awardStat(Stats.ITEM_USED.get(this));
        DiscordNotices.documentOpened(Reader, Stamp.Document(), Document, true);
        PacketDistributor.sendToPlayer(Reader, new OpenDossierPayload(Hand, true));
        return InteractionResultHolder.success(Unsealed);
    }

    @Override
    public Component getName(ItemStack Stack) {
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        Component Base = super.getName(Stack);
        return Stamp == null ? Base : Base.copy().withStyle(Look -> Look.withColor(Stamp.Classification().getColour() & 0xFFFFFF));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        if (Stamp == null) {
            Lines.add(Component.translatable("item.scp_unleashed.sealed_document.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        DocumentClass Classification = Stamp.Classification();
        Lines.add(Component.translatable("item.scp_unleashed.sealed_document.class", Classification.getName().copy()
                .withStyle(Look -> Look.withColor(Classification.getColour() & 0xFFFFFF).withBold(true))).withStyle(ChatFormatting.GRAY));
        AnomalyDocument Revealed = RevealCheck.getAsBoolean() ? AnomalyDocuments.lookup(Stamp.Document()) : null;
        if (Revealed != null) {
            Lines.add(Component.translatable("item.scp_unleashed.sealed_document.reveal", Revealed.designation(), Revealed.title())
                    .withStyle(Look -> Look.withColor(Classification.getColour() & 0xFFFFFF)));
        }
        Lines.add(Component.translatable("item.scp_unleashed.sealed_document.sealed").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        Lines.add(Component.translatable("item.scp_unleashed.sealed_document.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.sealed_document.details").withStyle(ChatFormatting.GOLD));
            Lines.add(Classification.getSummary().copy().withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.sealed_document.database").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
