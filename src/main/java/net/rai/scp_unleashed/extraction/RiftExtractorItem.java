package net.rai.scp_unleashed.extraction;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Extractable;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.registry.ModItems;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public class RiftExtractorItem extends Item {
    public static final int Durability = 64;
    public static final int UseDuration = 72000;
    public static final int SuccessCooldown = 16;
    public static final int GraceTicks = 6;
    private static final Map<LivingEntity, Tether> Tethers = new WeakHashMap<>();

    public RiftExtractorItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        User.startUsingItem(Hand);
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return UseDuration;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level World, LivingEntity User, ItemStack Stack, int RemainingTicks) {
        if (World.isClientSide) {
            return;
        }
        Entity Found = ExtractionTargeting.findTarget(User);
        Tether Previous = Tethers.get(User);
        if (!(Found instanceof Extractable Rift)) {
            if (Previous != null && ++Previous.Misses >= GraceTicks) {
                Tethers.remove(User);
                fizzle(World, User);
            }
            return;
        }
        if (Previous == null || Previous.TargetId != Found.getId()) {
            if (Previous != null) {
                fizzle(World, User);
            }
            Tethers.put(User, new Tether(Found.getId()));
        } else {
            Previous.Misses = 0;
        }
        float Progress = Math.min(1.0F, Rift.getExtraction() + ExtractionTargeting.stepFor(Rift.getRarity()));
        Rift.setExtraction(Progress);
        if (Progress >= 1.0F) {
            complete(World, User, Stack, Found, Rift);
        }
    }

    @Override
    public void onStopUsing(ItemStack Stack, LivingEntity User, int Count) {
        if (!User.level().isClientSide && Tethers.remove(User) != null) {
            fizzle(User.level(), User);
        }
    }

    private void complete(Level World, LivingEntity User, ItemStack Stack, Entity Found, Extractable Rift) {
        Tethers.remove(User);
        FragmentRarity Grade = Rift.getRarity();
        Vec3 Centre = Found.getBoundingBox().getCenter();
        Rift.collapse();
        ItemStack Fragment = Rift.createFragment();
        if (Fragment.isEmpty()) {
            Fragment = new ItemStack(ExtractionRegistry.fragmentFor(Grade));
        }
        if (User instanceof Player Holder) {
            if (!Holder.getInventory().add(Fragment)) {
                Holder.drop(Fragment, false);
            }
            Holder.getCooldowns().addCooldown(this, SuccessCooldown);
            Holder.awardStat(Stats.ITEM_USED.get(this));
        } else {
            User.spawnAtLocation(Fragment);
        }
        World.playSound(null, Centre.x, Centre.y, Centre.z, ExtractionRegistry.ExtractionSuccess.get(), SoundSource.PLAYERS, 1.0F, successPitch(Grade));
        Stack.hurtAndBreak(1, User, LivingEntity.getSlotForHand(User.getUsedItemHand()));
        User.stopUsingItem();
    }

    private static void fizzle(Level World, LivingEntity User) {
        World.playSound(null, User.getX(), User.getEyeY() - 0.3, User.getZ(), ExtractionRegistry.ExtractionFizzle.get(), SoundSource.PLAYERS, 0.8F, 0.9F + World.random.nextFloat() * 0.2F);
    }

    private static float successPitch(FragmentRarity Grade) {
        return 1.12F - 0.07F * (Grade.getTier() - 1);
    }

    @Override
    public boolean isValidRepairItem(ItemStack Stack, ItemStack Repair) {
        return Repair.is(ModItems.EnergumDust.get());
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack OldStack, ItemStack NewStack, boolean SlotChanged) {
        return SlotChanged || !ItemStack.isSameItem(OldStack, NewStack);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.rift_extractor.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.rift_extractor.range", (int) ExtractionTargeting.Range).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.rift_extractor.times",
                    seconds(FragmentRarity.Faint), seconds(FragmentRarity.Abyssal)).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.rift_extractor.steady").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.rift_extractor.repair").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private static final class Tether {
        private final int TargetId;
        private int Misses;

        private Tether(int TargetId) {
            this.TargetId = TargetId;
        }
    }

    private static String seconds(FragmentRarity Grade) {
        float Value = ExtractionTargeting.secondsFor(Grade);
        return Value == Math.floor(Value) ? Integer.toString((int) Value) : String.format(Locale.ROOT, "%.1f", Value);
    }
}
