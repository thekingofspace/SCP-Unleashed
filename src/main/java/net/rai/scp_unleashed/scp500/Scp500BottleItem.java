package net.rai.scp_unleashed.scp500;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp500BottleItem extends BlockItem {
    public static final String Prefix = "block.scp_unleashed.scp500_bottle";

    public Scp500BottleItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp500BottleItem;
    }

    public static int pills(ItemStack Stack) {
        Scp500Stock Carried = Stack.get(Scp500Registry.Stock.get());
        return Carried == null ? Scp500Stock.Full : Carried.Pills();
    }

    @Override
    public int getEntityLifespan(ItemStack Stack, Level World) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        int Left = pills(Stack);
        Lines.add(Left > 0 ? Component.translatable(Prefix + ".left", Left).withStyle(ChatFormatting.RED) : Component.translatable(Prefix + ".none").withStyle(ChatFormatting.DARK_GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + ".detail.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + ".detail.1").withStyle(ChatFormatting.GRAY));
        }
    }
}
