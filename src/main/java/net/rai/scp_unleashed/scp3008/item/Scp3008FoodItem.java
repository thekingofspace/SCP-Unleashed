package net.rai.scp_unleashed.scp3008.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;

import java.util.List;

public class Scp3008FoodItem extends Item {
    private final boolean Drink;
    private final String Tooltip;

    public Scp3008FoodItem(Properties Settings, boolean Drink, String Tooltip) {
        super(Settings);
        this.Drink = Drink;
        this.Tooltip = Tooltip;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return Drink ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return Drink ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Tooltip).withStyle(ChatFormatting.GRAY));
    }
}
