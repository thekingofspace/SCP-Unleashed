package net.rai.scp_unleashed.scp472;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp472BloodstoneItem extends BlockItem {
    public static final String Prefix = "block.scp_unleashed.scp472_bloodstone";
    public static final int Details = 2;

    public Scp472BloodstoneItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp472BloodstoneItem;
    }

    @Override
    public int getEntityLifespan(ItemStack Stack, Level World) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            for (int Index = 0; Index < Details; Index++) {
                Lines.add(Component.translatable(Prefix + ".detail." + Index).withStyle(Index == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            }
        }
    }
}
