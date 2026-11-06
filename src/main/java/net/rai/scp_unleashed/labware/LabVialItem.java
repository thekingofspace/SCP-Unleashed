package net.rai.scp_unleashed.labware;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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

import java.util.List;
import java.util.Objects;

public class LabVialItem extends Item {
    public static final double ItemEntityLift = 0.25;

    public LabVialItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public Component getName(ItemStack Stack) {
        ResourceLocation Sample = LabVials.sampleOf(Stack);
        boolean Open = LabVials.isOpen(Stack);
        if (Sample == null) {
            return Component.translatable(Open ? "item.scp_unleashed.lab_vial.open" : "item.scp_unleashed.lab_vial.sealed");
        }
        return Component.translatable(Open ? "item.scp_unleashed.lab_vial.open_sample" : "item.scp_unleashed.lab_vial.sealed_sample", LabOrganisms.nameOf(Sample));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (!User.isSecondaryUseActive() || LabVials.isOpen(Held)) {
            return InteractionResultHolder.pass(Held);
        }
        return InteractionResultHolder.sidedSuccess(breakSeal(World, User, Held), World.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        ItemStack Held = Context.getItemInHand();
        if (User == null || !User.isSecondaryUseActive() || LabVials.isOpen(Held)) {
            return LabOrganisms.find(LabVials.sampleOf(Held)).map(Organism -> Organism.useVialOn(Context, Held)).orElse(InteractionResult.PASS);
        }
        User.setItemInHand(Context.getHand(), breakSeal(Context.getLevel(), User, Held));
        return InteractionResult.sidedSuccess(Context.getLevel().isClientSide);
    }

    private ItemStack breakSeal(Level World, Player User, ItemStack Held) {
        if (World.isClientSide) {
            return Held;
        }
        ItemStack Opened = Held.copyWithCount(1);
        LabVials.open(Opened);
        User.awardStat(Stats.ITEM_USED.get(this));
        LabwareSounds.play(World, null, User.getX(), User.getY(), User.getZ(), LabwareSounds.Unsealed, SoundSource.PLAYERS, LabwareSounds.pitch(World));
        return ItemUtils.createFilledResult(Held, User, Opened);
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Carrier, int Slot, boolean Selected) {
        if (!(World instanceof ServerLevel Server) || !LabVials.isOpenCulture(Stack) || !LabExposure.isPulse(World, Carrier.getId())) {
            return;
        }
        ResourceLocation Sample = LabVials.sampleOf(Stack);
        if (Carrier instanceof Player User && firstOpenCulture(User.getInventory(), Sample) != Stack) {
            return;
        }
        LabOrganisms.expose(Server, Carrier.getEyePosition(), LabExposure.NothingIgnored, Sample);
    }

    private static ItemStack firstOpenCulture(Inventory Carried, ResourceLocation Sample) {
        for (int Index = 0; Index < Carried.getContainerSize(); Index++) {
            ItemStack Candidate = Carried.getItem(Index);
            if (LabVials.isOpenCulture(Candidate) && Objects.equals(LabVials.sampleOf(Candidate), Sample)) {
                return Candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack Stack, ItemEntity Dropped) {
        if (Dropped.level() instanceof ServerLevel Server && LabVials.isOpenCulture(Stack) && LabExposure.isPulse(Server, Dropped.getId())) {
            LabOrganisms.expose(Server, Dropped.position().add(0.0, ItemEntityLift, 0.0), LabExposure.NothingIgnored, LabVials.sampleOf(Stack));
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        boolean Open = LabVials.isOpen(Stack);
        ResourceLocation Sample = LabVials.sampleOf(Stack);
        if (Sample == null) {
            Lines.add(Component.translatable("item.scp_unleashed.lab_vial.empty").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            Lines.add(Component.translatable("item.scp_unleashed.lab_vial.contents", LabOrganisms.styledNameOf(Sample)).withStyle(ChatFormatting.GRAY));
        }
        Lines.add(Component.translatable(Open ? "item.scp_unleashed.lab_vial.state.open" : "item.scp_unleashed.lab_vial.state.sealed")
                .withStyle(Open ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        if (Open && LabOrganisms.isKnown(Sample)) {
            Lines.add(Component.translatable("item.scp_unleashed.lab_vial.state.source", (int) LabOrganisms.radiusOf(Sample)).withStyle(ChatFormatting.RED));
        }
        if (ItemTooltips.showDetails(Lines)) {
            if (Open) {
                Lines.add(Component.translatable("item.scp_unleashed.lab_vial.detail.open").withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("item.scp_unleashed.lab_vial.detail.holder").withStyle(ChatFormatting.GRAY));
            } else {
                Lines.add(Component.translatable("item.scp_unleashed.lab_vial.detail.sealed").withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("item.scp_unleashed.lab_vial.detail.unseal").withStyle(ChatFormatting.GRAY));
            }
            if (Sample != null) {
                Lines.add(Component.translatable("item.scp_unleashed.lab_vial.detail.pour").withStyle(ChatFormatting.AQUA));
            }
            Lines.add(Component.translatable("item.scp_unleashed.lab_vial.detail.storage").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
