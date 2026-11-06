package net.rai.scp_unleashed.space;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

public class AirCanisterItem extends Item {
    public static final int Capacity = 600;
    public static final int StackSize = 16;
    public static final int BarColour = 0x5FC8F0;

    public AirCanisterItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (SpaceAir.refill(User, Stack)) {
            return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
        }
        return InteractionResultHolder.pass(Stack);
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        return SpaceAir.canisterAir(Stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        return Math.round(13.0F * Mth.clamp(SpaceAir.canisterAir(Stack) / (float) Capacity, 0.0F, 1.0F));
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        return BarColour;
    }

    @Override
    public Component getName(ItemStack Stack) {
        return SpaceAir.canisterAir(Stack) > 0 ? super.getName(Stack) : Component.translatable(getDescriptionId() + ".empty");
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        int Air = SpaceAir.canisterAir(Stack);
        if (Air > 0) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.space.air", Math.round(Air * 100.0F / Capacity)).withStyle(ChatFormatting.AQUA));
        } else {
            Lines.add(Component.translatable("tooltip.scp_unleashed.space.canister_empty").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(getDescriptionId() + ".detail.fill").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(getDescriptionId() + ".detail.fit").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(getDescriptionId() + ".detail.duration", Capacity / 60).withStyle(ChatFormatting.GOLD));
        }
    }
}
