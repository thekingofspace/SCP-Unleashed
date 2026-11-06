package net.rai.scp_unleashed.scp066;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class Scp066TreatItem extends Item {
    public Scp066TreatItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (this == Scp066Registry.Cupcake.get()) {
            Lines.add(Component.translatable("item.scp_unleashed.scp066_cupcake.cheer", Scp066Registry.CupcakeCheerTicks / 20).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
