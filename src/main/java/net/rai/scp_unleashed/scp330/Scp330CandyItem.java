package net.rai.scp_unleashed.scp330;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class Scp330CandyItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp330_candy.";
    private final Scp330Colour Colour;

    public Scp330CandyItem(Scp330Colour Colour, Properties Settings) {
        super(Settings);
        this.Colour = Colour;
    }

    public Scp330Colour colour() {
        return Colour;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity Eater) {
        ItemStack Left = super.finishUsingItem(Stack, World, Eater);
        if (!World.isClientSide) {
            Scp330Candies.eat(Eater, Colour);
        }
        return Left;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(Prefix + Colour.id()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
