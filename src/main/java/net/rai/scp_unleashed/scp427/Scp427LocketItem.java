package net.rai.scp_unleashed.scp427;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp427LocketItem extends Item {
    public static final int UseTicks = 72000;
    public static final String Key = "item.scp_unleashed.scp427_locket.";

    public Scp427LocketItem(Properties Properties) {
        super(Properties);
    }

    public static boolean is(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.is(Scp427Registry.Locket.get());
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity Holder) {
        return UseTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (Hand != InteractionHand.MAIN_HAND || Scp427Exposure.isTransforming(User)) {
            return InteractionResultHolder.pass(Stack);
        }
        User.startUsingItem(Hand);
        if (!World.isClientSide) {
            User.awardStat(Stats.ITEM_USED.get(this));
            Scp427Registry.play(World, User.getEyePosition().subtract(0.0, 0.4, 0.0), Scp427Registry.OpenLayers, SoundSource.PLAYERS, 0.55f, 1.0f);
        }
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public void onStopUsing(ItemStack Stack, LivingEntity Holder, int Count) {
        if (!Holder.level().isClientSide) {
            Scp427Registry.play(Holder.level(), Holder.getEyePosition().subtract(0.0, 0.4, 0.0), Scp427Registry.CloseLayers, SoundSource.PLAYERS, 0.55f, 1.0f);
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack OldStack, ItemStack NewStack, boolean SlotChanged) {
        return SlotChanged || !ItemStack.isSameItem(OldStack, NewStack);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "detail.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "detail.1").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "detail.2").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
