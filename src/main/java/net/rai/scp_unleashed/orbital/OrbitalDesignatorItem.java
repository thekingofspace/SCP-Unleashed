package net.rai.scp_unleashed.orbital;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;
import java.util.function.LongSupplier;

public class OrbitalDesignatorItem extends Item {
    public static final int BarWidth = 13;
    public static final int ChargingColour = 0xD8402C;
    public static final int NearlyColour = 0xFFB040;
    private static LongSupplier Clock = () -> Long.MIN_VALUE;

    public OrbitalDesignatorItem(Properties Settings) {
        super(Settings);
    }

    public static void setClock(LongSupplier Supplier) {
        Clock = Supplier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (User instanceof ServerPlayer Server) {
            OrbitalFiring.use(Server, Stack);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide());
    }

    private static OrbitalRecharge charging(ItemStack Stack) {
        OrbitalRecharge Recharge = Stack.get(OrbitalRegistry.Recharge.get());
        long Now = Clock.getAsLong();
        return Recharge != null && Now != Long.MIN_VALUE && Recharge.charging(Now) ? Recharge : null;
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        return charging(Stack) != null;
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        OrbitalRecharge Recharge = charging(Stack);
        return Recharge == null ? BarWidth : Math.round(BarWidth * Recharge.progress(Clock.getAsLong()));
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        OrbitalRecharge Recharge = charging(Stack);
        float Progress = Recharge == null ? 1.0F : Recharge.progress(Clock.getAsLong());
        return FastColor.ARGB32.lerp(Mth.clamp(Progress, 0.0F, 1.0F), ChargingColour, NearlyColour) & 0xFFFFFF;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.orbital_designator.tooltip").withStyle(ChatFormatting.GRAY));
        OrbitalLink Link = Stack.get(OrbitalRegistry.Link.get());
        if (Link == null) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.orbital.unlinked").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            Lines.add(Component.translatable("tooltip.scp_unleashed.orbital.linked_to", Link.Name()).withStyle(ChatFormatting.DARK_AQUA));
            OrbitalRecharge Recharge = charging(Stack);
            if (Recharge != null) {
                Lines.add(Component.translatable("tooltip.scp_unleashed.orbital.recharging", Recharge.secondsLeft(Clock.getAsLong())).withStyle(ChatFormatting.GOLD));
            } else {
                Lines.add(Component.translatable("tooltip.scp_unleashed.orbital.ready").withStyle(ChatFormatting.GREEN));
            }
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.orbital.designator_use").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("tooltip.scp_unleashed.orbital.designator_warning").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("tooltip.scp_unleashed.orbital.designator_owner").withStyle(ChatFormatting.GOLD));
        }
    }
}
