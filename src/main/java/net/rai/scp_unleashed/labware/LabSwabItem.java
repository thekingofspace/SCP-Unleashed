package net.rai.scp_unleashed.labware;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.ArrayList;
import java.util.List;

public class LabSwabItem extends Item {
    public static final double SwabRadius = 4.0;
    public static final double ItemEntityLift = 0.15;

    public LabSwabItem(Properties Settings) {
        super(Settings);
    }

    public static List<LabOrganism> organismsOn(ItemStack Stack) {
        LabSwabRecord Record = LabVials.isUsedSwab(Stack) ? LabVials.recordOf(Stack) : null;
        if (Record == null) {
            return List.of();
        }
        List<LabOrganism> Found = new ArrayList<>();
        for (ResourceLocation Finding : Record.Findings()) {
            LabOrganisms.find(Finding).filter(Organism -> !Found.contains(Organism)).ifPresent(Found::add);
        }
        return Found;
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Carrier, int Slot, boolean Selected) {
        if (!(World instanceof ServerLevel Server) || !LabExposure.isPulse(World, Carrier.getId())) {
            return;
        }
        for (LabOrganism Organism : organismsOn(Stack)) {
            if (Carrier instanceof Player User && firstCarrying(User.getInventory(), Organism) != Stack) {
                continue;
            }
            LabOrganisms.spread(Server, Carrier.getEyePosition(), LabExposure.NothingIgnored, Organism, Math.min(SwabRadius, Organism.getRadius()), null);
        }
    }

    private static ItemStack firstCarrying(Inventory Carried, LabOrganism Organism) {
        for (int Index = 0; Index < Carried.getContainerSize(); Index++) {
            ItemStack Candidate = Carried.getItem(Index);
            if (organismsOn(Candidate).contains(Organism)) {
                return Candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack Stack, ItemEntity Dropped) {
        if (Dropped.level() instanceof ServerLevel Server && LabExposure.isPulse(Server, Dropped.getId())) {
            for (LabOrganism Organism : organismsOn(Stack)) {
                LabOrganisms.spread(Server, Dropped.position().add(0.0, ItemEntityLift, 0.0), LabExposure.NothingIgnored, Organism,
                        Math.min(SwabRadius, Organism.getRadius()), null);
            }
        }
        return false;
    }

    @Override
    public Component getName(ItemStack Stack) {
        LabSwabRecord Record = LabVials.recordOf(Stack);
        if (Record == null) {
            return Component.translatable("item.scp_unleashed.lab_swab");
        }
        if (Record.Labelled() && !Record.isBlank()) {
            return Component.translatable("item.scp_unleashed.lab_swab.labelled", LabOrganisms.nameOf(Record.Findings().get(0)));
        }
        return Component.translatable("item.scp_unleashed.lab_swab.used");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (User.isSecondaryUseActive()) {
            if (!LabVials.isSterileSwab(Held)) {
                return InteractionResultHolder.pass(Held);
            }
            swab(User, Hand, User);
            return InteractionResultHolder.sidedSuccess(User.getItemInHand(Hand), World.isClientSide);
        }
        if (!transferToHeldVial(User, Hand)) {
            return InteractionResultHolder.pass(Held);
        }
        return InteractionResultHolder.sidedSuccess(User.getItemInHand(Hand), World.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        if (User != null && !User.isSecondaryUseActive() && LabVials.isSterileSwab(Context.getItemInHand())) {
            return LabSurfaces.swab(Context);
        }
        if (User == null || User.isSecondaryUseActive() || !transferToHeldVial(User, Context.getHand())) {
            return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(Context.getLevel().isClientSide);
    }

    public static boolean transferToHeldVial(Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (!LabVials.isUsedSwab(Held)) {
            return false;
        }
        InteractionHand Other = Hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack Target = User.getItemInHand(Other);
        if (!LabVials.isEmptyVial(Target)) {
            return false;
        }
        Level World = User.level();
        LabSwabRecord Record = LabVials.recordOf(Held);
        if (Record == null) {
            return false;
        }
        if (!Record.Labelled()) {
            announce(World, User, "message.scp_unleashed.labware.swab_unread");
            return true;
        }
        ResourceLocation Sample = Record.cultureSample();
        if (Sample == null) {
            announce(World, User, "message.scp_unleashed.labware.swab_nothing");
            return true;
        }
        if (!World.isClientSide) {
            ItemStack Filled = Target.copyWithCount(1);
            LabVials.setSample(Filled, Sample);
            User.setItemInHand(Other, ItemUtils.createFilledResult(Target, User, Filled));
            Held.consume(1, User);
            User.awardStat(Stats.ITEM_USED.get(LabwareRegistry.Swab.get()));
            User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.vial_filled", LabOrganisms.nameOf(Sample)).withStyle(ChatFormatting.GRAY), true);
            LabwareSounds.play(World, null, User.getX(), User.getY(0.6), User.getZ(), LabwareSounds.Streaked, SoundSource.PLAYERS, LabwareSounds.pitch(World));
        }
        return true;
    }

    private static void announce(Level World, Player User, String Key) {
        if (!World.isClientSide) {
            User.displayClientMessage(Component.translatable(Key).withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static InteractionResult swab(Player User, InteractionHand Hand, LivingEntity Target) {
        Level World = User.level();
        ItemStack Held = User.getItemInHand(Hand);
        if (!LabVials.isSterileSwab(Held)) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            LabSwabRecord Record = new LabSwabRecord(false, Target.getName().copy(), LabOrganisms.detect(Target), World.getDayTime());
            User.setItemInHand(Hand, ItemUtils.createFilledResult(Held, User, LabVials.usedSwab(Record)));
            User.awardStat(Stats.ITEM_USED.get(Held.getItem()));
            User.displayClientMessage(Component.translatable(Target == User ? "message.scp_unleashed.labware.swabbed_self" : "message.scp_unleashed.labware.swabbed",
                    Target.getName()).withStyle(ChatFormatting.GRAY), true);
            LabwareSounds.play(World, null, Target.getX(), Target.getY(0.6), Target.getZ(), LabwareSounds.Swabbed, SoundSource.PLAYERS, LabwareSounds.pitch(World));
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static LabSwabRecord fromDish(ResourceLocation Sample, long Taken) {
        return new LabSwabRecord(true, Component.translatable("labware.scp_unleashed.subject.dish", LabOrganisms.nameOf(Sample)), List.of(Sample), Taken);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        LabSwabRecord Record = LabVials.recordOf(Stack);
        if (Record == null) {
            Lines.add(Component.translatable("item.scp_unleashed.lab_swab.sterile").withStyle(ChatFormatting.AQUA));
            if (ItemTooltips.showDetails(Lines)) {
                Lines.add(Component.translatable("item.scp_unleashed.lab_swab.detail.creature").withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("item.scp_unleashed.lab_swab.detail.self").withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("item.scp_unleashed.lab_swab.detail.culture").withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("item.scp_unleashed.lab_swab.detail.copy").withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("item.scp_unleashed.lab_swab.detail.single").withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
        Lines.add(Component.translatable("item.scp_unleashed.lab_swab.source", Record.Subject()).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("item.scp_unleashed.lab_swab.taken", LabClock.stamp(Record.Taken())).withStyle(ChatFormatting.DARK_GRAY));
        if (!Record.Labelled()) {
            Lines.add(Component.translatable("item.scp_unleashed.lab_swab.unread").withStyle(ChatFormatting.GOLD));
        } else if (Record.isBlank()) {
            Lines.add(Component.translatable("item.scp_unleashed.lab_swab.blank").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            Lines.add(Component.translatable("item.scp_unleashed.lab_swab.carrying", LabOrganisms.styledNameOf(Record.Findings().get(0))).withStyle(ChatFormatting.GRAY));
            if (!organismsOn(Stack).isEmpty()) {
                Lines.add(Component.translatable("item.scp_unleashed.lab_swab.live", (int) SwabRadius).withStyle(ChatFormatting.RED));
            }
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.lab_swab.detail.analyzer").withStyle(ChatFormatting.GRAY));
            if (Record.Labelled()) {
                Lines.add(Component.translatable("item.scp_unleashed.lab_swab.detail.dish").withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("item.scp_unleashed.lab_swab.detail.copy").withStyle(ChatFormatting.AQUA));
                Lines.add(Component.translatable("item.scp_unleashed.lab_swab.detail.rack").withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
