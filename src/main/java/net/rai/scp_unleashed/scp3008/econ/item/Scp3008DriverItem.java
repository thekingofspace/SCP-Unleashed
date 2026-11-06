package net.rai.scp_unleashed.scp3008.econ.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;

import java.util.List;

public class Scp3008DriverItem extends Scp3008StoreItem {
    public static final int Uses = 64;
    public static final float FurnitureSpeed = 4.0f;
    public static final int Enchantability = 10;

    public Scp3008DriverItem(Properties Settings) {
        super(Settings);
    }

    public static Tool createTool() {
        return new Tool(List.of(Tool.Rule.overrideSpeed(Scp3008HammerItem.Furniture, FurnitureSpeed)), 1.0f, 0);
    }

    @Override
    public boolean isValidRepairItem(ItemStack Stack, ItemStack Repair) {
        return Repair.is(Scp3008EconRegistry.HexFittings.get());
    }

    @Override
    public int getEnchantmentValue() {
        return Enchantability;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(getDescriptionId() + ".details").withStyle(ChatFormatting.GOLD));
        }
    }
}
