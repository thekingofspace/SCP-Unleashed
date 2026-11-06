package net.rai.scp_unleashed.sealedcase;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SealedCaseItem extends Item {
    public static final int UseCooldown = 12;
    public static final String Prefix = "item.scp_unleashed.sealed_case.";

    public SealedCaseItem(Properties Settings) {
        super(Settings);
    }

    @Nullable
    public static SealedContents contents(ItemStack Case) {
        return Case.get(SealedCaseRegistry.Contents.get());
    }

    public static boolean isSealed(ItemStack Case) {
        return Case.getItem() instanceof SealedCaseItem && Case.has(SealedCaseRegistry.Contents.get());
    }

    public static ItemStack sealedWith(ItemStack Inside) {
        ItemStack Case = new ItemStack(SealedCaseRegistry.SealedCase.get());
        if (!Inside.isEmpty()) {
            Case.set(SealedCaseRegistry.Contents.get(), new SealedContents(Inside));
        }
        return Case;
    }

    public static boolean canSeal(ItemStack Candidate) {
        return !Candidate.isEmpty() && !(Candidate.getItem() instanceof SealedCaseItem) && Candidate.canFitInsideContainerItems();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack Stack, UseOnContext Context) {
        return CaseTransfer.use(Stack, Context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Case = User.getItemInHand(Hand);
        if (User.isSecondaryUseActive()) {
            return seal(World, User, Hand, Case);
        }
        return open(World, User, Case);
    }

    private InteractionResultHolder<ItemStack> open(Level World, Player User, ItemStack Case) {
        SealedContents Inside = contents(Case);
        if (Inside == null) {
            if (!World.isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.sealed_case.empty").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(Case);
        }
        if (!World.isClientSide) {
            ItemStack Released = Inside.stack();
            Case.remove(SealedCaseRegistry.Contents.get());
            Component Name = describe(Released);
            ItemHandlerHelper.giveItemToPlayer(User, Released);
            play(World, User, SealedCaseRegistry.Unlatch.get(), 1.0f, 0.95f + World.random.nextFloat() * 0.08f);
            play(World, User, SealedCaseRegistry.Hinge.get(), 0.9f, 0.95f + World.random.nextFloat() * 0.08f);
            play(World, User, SealedCaseRegistry.Release.get(), 0.7f, 1.0f);
            puff(World, User);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.sealed_case.opened", Name).withStyle(ChatFormatting.GOLD), true);
            User.awardStat(Stats.ITEM_USED.get(this));
        }
        User.getCooldowns().addCooldown(this, UseCooldown);
        return InteractionResultHolder.sidedSuccess(Case, World.isClientSide);
    }

    private InteractionResultHolder<ItemStack> seal(Level World, Player User, InteractionHand Hand, ItemStack Case) {
        InteractionHand OtherHand = Hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack Offered = User.getItemInHand(OtherHand);
        SealedContents Inside = contents(Case);
        Component Refusal = null;
        if (Inside != null) {
            Refusal = Component.translatable("message.scp_unleashed.sealed_case.full", describe(Inside.stack()));
        } else if (Offered.isEmpty()) {
            Refusal = Component.translatable("message.scp_unleashed.sealed_case.nothing");
        } else if (!canSeal(Offered)) {
            Refusal = Component.translatable("message.scp_unleashed.sealed_case.refused", Offered.getHoverName());
        }
        if (Refusal != null) {
            if (!World.isClientSide) {
                User.displayClientMessage(Refusal.copy().withStyle(ChatFormatting.RED), true);
                play(World, User, SealedCaseRegistry.Deny.get(), 0.8f, 1.0f);
            }
            return InteractionResultHolder.fail(Case);
        }
        if (!World.isClientSide) {
            ItemStack Stored = Offered.copy();
            Case.set(SealedCaseRegistry.Contents.get(), new SealedContents(Stored));
            User.setItemInHand(OtherHand, ItemStack.EMPTY);
            play(World, User, SealedCaseRegistry.Seal.get(), 1.0f, 0.95f + World.random.nextFloat() * 0.08f);
            play(World, User, SealedCaseRegistry.Clamp.get(), 0.9f, 0.95f + World.random.nextFloat() * 0.08f);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.sealed_case.sealed", describe(Stored)).withStyle(ChatFormatting.GOLD), true);
            User.awardStat(Stats.ITEM_USED.get(this));
        }
        User.getCooldowns().addCooldown(this, UseCooldown);
        return InteractionResultHolder.sidedSuccess(Case, World.isClientSide);
    }

    private static void play(Level World, Player User, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Sound, SoundSource.PLAYERS, Volume, Pitch);
    }

    private static void puff(Level World, Player User) {
        if (World instanceof ServerLevel Server) {
            Vec3 Ahead = User.getEyePosition().add(User.getLookAngle().scale(0.6)).subtract(0.0, 0.45, 0.0);
            Server.sendParticles(ParticleTypes.WHITE_SMOKE, Ahead.x, Ahead.y, Ahead.z, 5, 0.12, 0.06, 0.12, 0.01);
        }
    }

    public static MutableComponent describe(ItemStack Stack) {
        MutableComponent Name = Stack.getHoverName().copy().withStyle(Stack.getRarity().getStyleModifier());
        return Stack.getCount() > 1 ? Component.translatable(Prefix + "counted", Name, Stack.getCount()) : Name;
    }

    @Override
    public void onDestroyed(ItemEntity Drop) {
        SealedContents Inside = contents(Drop.getItem());
        if (Inside != null) {
            Drop.getItem().remove(SealedCaseRegistry.Contents.get());
            ItemUtils.onContainerDestroyed(Drop, List.of(Inside.stack()));
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        SealedContents Inside = contents(Stack);
        if (Inside == null) {
            Lines.add(Component.translatable(Prefix + "empty").withStyle(ChatFormatting.GRAY));
        } else {
            ItemStack Held = Inside.stack();
            Lines.add(Component.translatable(Prefix + "holds", describe(Held)).withStyle(ChatFormatting.GRAY));
            if (Held.is(SealedCaseTags.Hazardous)) {
                Lines.add(Component.translatable(Prefix + "hazard").withStyle(ChatFormatting.RED));
            }
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "inert").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "open").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "seal").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "transfer").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "retrieve").withStyle(ChatFormatting.GOLD));
        }
    }
}
