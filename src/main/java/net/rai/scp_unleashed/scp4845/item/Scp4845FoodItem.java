package net.rai.scp_unleashed.scp4845.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;

import java.util.List;

public class Scp4845FoodItem extends Item {
    private final Scp4845MenuItem Entry;

    public Scp4845FoodItem(Scp4845MenuItem Entry, Properties Settings) {
        super(Settings);
        this.Entry = Entry;
    }

    public Scp4845MenuItem entry() {
        return Entry;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return Entry.Drink ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity Eater) {
        if (!World.isClientSide) {
            if (Entry == Scp4845MenuItem.CrunchyTaco || Entry == Scp4845MenuItem.Nachos) {
                Scp4845Registry.playLayers(World, null, Eater.getX(), Eater.getEyeY(), Eater.getZ(), Scp4845Registry.CrunchLayers, SoundSource.PLAYERS, 0.7f,
                        0.95f + World.getRandom().nextFloat() * 0.15f);
            } else if (Entry == Scp4845MenuItem.LunarBlast) {
                Scp4845Registry.playLayers(World, null, Eater.getX(), Eater.getEyeY(), Eater.getZ(), Scp4845Registry.FizzLayers, SoundSource.PLAYERS, 0.6f, 1.0f);
            }
        }
        return super.finishUsingItem(Stack, World, Eater);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed." + Entry.Id + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
