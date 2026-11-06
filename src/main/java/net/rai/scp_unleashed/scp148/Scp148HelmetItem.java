package net.rai.scp_unleashed.scp148;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp148HelmetItem extends ArmorItem {
    public static final String Key = "item.scp_unleashed.scp148_helmet.";

    public Scp148HelmetItem(Holder<ArmorMaterial> Material, Properties Settings) {
        super(Material, Type.HELMET, Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp148HelmetItem;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GREEN));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "details").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Key + "alone").withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
