package net.rai.scp_unleashed.scp085;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp085SheetItem extends BlockItem {
    public Scp085SheetItem(Block Placed, Properties Properties) {
        super(Placed, Properties);
    }

    public boolean isHomeSheet() {
        return getBlock() instanceof Scp085HomeSheetBlock;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        if (isHomeSheet()) {
            Lines.add(Component.translatable("item.scp_unleashed.scp085_home_sheet.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            if (Scp085Resident.shown()) {
                Lines.add(Component.translatable("item.scp_unleashed.scp085_home_sheet.resident").withStyle(ChatFormatting.DARK_AQUA));
            }
            if (ItemTooltips.showDetails(Lines)) {
                Lines.add(Component.translatable("item.scp_unleashed.scp085_home_sheet.hang").withStyle(ChatFormatting.GOLD));
                Lines.add(Component.translatable("item.scp_unleashed.scp085_home_sheet.visit").withStyle(ChatFormatting.GOLD));
                Lines.add(Component.translatable("item.scp_unleashed.scp085_home_sheet.greet").withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        Scp085Scene Scene = Scp085DrawingBlock.sceneOf(Stack);
        Component Title = Scene == null
                ? Component.translatable("item.scp_unleashed.scp085_drawing.unknown")
                : Component.translatable(Scene.titleKey());
        Lines.add(Component.translatable("item.scp_unleashed.scp085_drawing.tooltip", Title.copy().withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp085_drawing.hang").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp085_drawing.visit").withStyle(ChatFormatting.GOLD));
        }
    }
}
