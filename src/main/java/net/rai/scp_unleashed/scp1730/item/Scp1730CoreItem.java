package net.rai.scp_unleashed.scp1730.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;

import java.util.List;

public class Scp1730CoreItem extends Item {
    public static final int HumEvery = 160;

    public Scp1730CoreItem(Properties Props) {
        super(Props);
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (!World.isClientSide && (World.getGameTime() + Holder.getId()) % HumEvery == 0) {
            World.playSound(null, Holder.getX(), Holder.getY(), Holder.getZ(), Scp1730Registry.CoreHum.get(), SoundSource.PLAYERS, 0.35f, 0.7f + World.random.nextFloat() * 0.1f);
        }
    }

    @Override
    public boolean isFoil(ItemStack Stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
