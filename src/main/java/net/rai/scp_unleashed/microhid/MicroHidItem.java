package net.rai.scp_unleashed.microhid;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;
import java.util.Locale;

public class MicroHidItem extends GunItem {
    public static final String Prefix = "item.scp_unleashed.micro_hid";
    public static final String TooltipPrefix = "tooltip.scp_unleashed.micro_hid.";
    public static final int EnergyBarColour = 0x5AA8FF;
    public static final int LowBarColour = 0xE5534B;

    public MicroHidItem(Properties Properties) {
        super(GunKind.MicroHid, Properties);
    }

    public static boolean isMicroHid(ItemStack Stack) {
        return Stack.getItem() instanceof MicroHidItem;
    }

    public static ItemStack charged(int Energy) {
        ItemStack Stack = new ItemStack(MicroHidRegistry.MicroHid.get());
        MicroHidEnergy.set(Stack, Energy);
        return Stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (Hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(Stack);
        }
        if (!MicroHidEnergy.canFire(Stack)) {
            if (!World.isClientSide) {
                MicroHidBeam.dryFire(User, Stack);
            }
            return InteractionResultHolder.fail(Stack);
        }
        User.setSprinting(false);
        User.startUsingItem(Hand);
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public void onUseTick(Level World, LivingEntity User, ItemStack Stack, int Remaining) {
        if (World instanceof ServerLevel ServerWorld) {
            MicroHidBeam.tick(ServerWorld, User, Stack, getUseDuration(Stack, User) - Remaining);
        }
    }

    @Override
    public void onStopUsing(ItemStack Stack, LivingEntity User, int Remaining) {
        if (!User.level().isClientSide) {
            MicroHidBeam.stop(User, Stack, getUseDuration(Stack, User) - Remaining);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        return Math.round(13.0F * MicroHidEnergy.fraction(Stack));
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        return MicroHidEnergy.fraction(Stack) < MicroHidEnergy.LowShare ? LowBarColour : EnergyBarColour;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        GunStats Stats = getKind().getStats();
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        int Percent = MicroHidEnergy.percent(Stack);
        ChatFormatting Shade = Percent <= 0 ? ChatFormatting.DARK_RED : MicroHidEnergy.fraction(Stack) < MicroHidEnergy.LowShare ? ChatFormatting.GOLD : ChatFormatting.AQUA;
        Lines.add(Component.translatable(TooltipPrefix + "charge", Percent).withStyle(Shade));
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        Lines.add(Component.translatable(TooltipPrefix + "damage", number(Stats.getDamage()), number(20.0F / Stats.getInterval())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "range", Math.round(Stats.getRange())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "spin", number(MicroHidEnergy.spinSeconds())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "battery", number(MicroHidEnergy.fireSeconds())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "controls", Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(TooltipPrefix + "recharge").withStyle(ChatFormatting.GRAY));
    }

    private static String number(float Value) {
        return Value == Math.floor(Value) ? Integer.toString((int) Value) : String.format(Locale.ROOT, "%.1f", Value);
    }
}
