package net.rai.scp_unleashed.scp063;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp063ToothbrushItem extends Item {
    public static final int UseTicks = 72000;
    public static final String Key = "item.scp_unleashed.scp063_toothbrush.";

    public Scp063ToothbrushItem(Properties Properties) {
        super(Properties);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.is(Scp063Registry.Toothbrush.get());
    }

    public static boolean isBrushing(LivingEntity Holder) {
        return Holder.isUsingItem() && is(Holder.getUseItem());
    }

    public static boolean isBrushingTeeth(LivingEntity Holder) {
        return isBrushing(Holder) && !Scp063Scrubbing.isScrubbing(Holder);
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity Holder) {
        return UseTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.CUSTOM;
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        if (User == null) {
            return InteractionResult.PASS;
        }
        if (User.isSecondaryUseActive()) {
            return Scp063RestingBlock.placeFrom(Context);
        }
        if (Context.getHand() != InteractionHand.MAIN_HAND || Scp063Scrubbing.aim(User) == null) {
            return InteractionResult.PASS;
        }
        Scp063Scrubbing.begin(User, Context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        Scp063Scrubbing.mark(User, false);
        if (User instanceof ServerPlayer Brusher) {
            Scp063Brushing.begin(Brusher);
        }
        return ItemUtils.startUsingInstantly(World, User, Hand);
    }

    @Override
    public void onUseTick(Level World, LivingEntity Holder, ItemStack Stack, int Remaining) {
        int Used = getUseDuration(Stack, Holder) - Remaining;
        if (Scp063Scrubbing.isScrubbing(Holder)) {
            if (Holder instanceof Player User) {
                Scp063Scrubbing.tick(World, User, Used);
            }
            return;
        }
        if (Holder instanceof ServerPlayer Brusher) {
            Scp063Brushing.tick(Brusher, Used);
        }
    }

    @Override
    public void releaseUsing(ItemStack Stack, Level World, LivingEntity Holder, int Remaining) {
        finish(Holder);
    }

    @Override
    public void onStopUsing(ItemStack Stack, LivingEntity Holder, int Count) {
        finish(Holder);
    }

    private static void finish(LivingEntity Holder) {
        if (Holder instanceof Player User && !User.level().isClientSide) {
            Scp063Scrubbing.stop(User);
            Scp063Scrubbing.mark(User, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "use").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "safe", Scp063Brushing.SoreTicks / Scp063Brushing.TicksPerSecond).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "sore").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "toothless", Scp063Brushing.ToothlessTicks / Scp063Brushing.TicksPerSecond).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "rest", Scp063Brushing.RestPerTick).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "scrub").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "set").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "leaves").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
