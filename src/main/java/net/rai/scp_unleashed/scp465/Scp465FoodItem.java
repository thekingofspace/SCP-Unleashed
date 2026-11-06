package net.rai.scp_unleashed.scp465;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;

import java.util.List;

public class Scp465FoodItem extends Item {
    public enum Kind {
        Chocolate,
        Punch,
        Cupcake,
        Chips
    }

    private final Kind Variety;

    public Scp465FoodItem(Kind Variety, Properties Settings) {
        super(Settings);
        this.Variety = Variety;
    }

    public Kind variety() {
        return Variety;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return Variety == Kind.Punch ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return Scp465Registry.Gulp.get();
    }

    @Override
    public SoundEvent getEatingSound() {
        return switch (Variety) {
            case Punch -> Scp465Registry.Gulp.get();
            case Chips -> Scp465Registry.Crunch.get();
            default -> super.getEatingSound();
        };
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
