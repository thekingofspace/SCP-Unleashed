package net.rai.scp_unleashed.scp458;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class Scp458SliceItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp458_slice";
    public static final float CrustVolume = 0.45f;

    public Scp458SliceItem(Properties Settings) {
        super(Settings);
    }

    public static Scp458Pizza pizza(ItemStack Stack) {
        return Stack.getOrDefault(Scp458Registry.Pizza.get(), Scp458Pizza.Cheese);
    }

    @Override
    public Component getName(ItemStack Stack) {
        return Component.translatable(Prefix + ".named", pizza(Stack).displayName());
    }

    @Override
    public SoundEvent getEatingSound() {
        return Scp458Registry.Munch.get();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity Eater) {
        if (!World.isClientSide) {
            float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
            World.playSound(null, Eater.getX(), Eater.getEyeY(), Eater.getZ(), Scp458Registry.MunchCrust.get(), SoundSource.PLAYERS, CrustVolume, Pitch);
        }
        return super.finishUsingItem(Stack, World, Eater);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
