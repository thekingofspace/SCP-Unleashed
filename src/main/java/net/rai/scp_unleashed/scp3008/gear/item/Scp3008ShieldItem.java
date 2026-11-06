package net.rai.scp_unleashed.scp3008.gear.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;

import java.util.List;

public class Scp3008ShieldItem extends ShieldItem {
    public static final int Uses = 220;

    public Scp3008ShieldItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public String getDescriptionId(ItemStack Stack) {
        return getDescriptionId();
    }

    @Override
    public boolean isValidRepairItem(ItemStack Stack, ItemStack Repair) {
        return Repair.is(Scp3008EconRegistry.Particleboard.get()) || Repair.is(Scp3008GearRegistry.CabinetDoor.get());
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
