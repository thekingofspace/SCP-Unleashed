package net.rai.scp_unleashed.dclass;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class CloningBloodItem extends Item {
    private final CloningKind Kind;

    public CloningBloodItem(CloningKind Kind, Properties Settings) {
        super(Settings);
        this.Kind = Kind;
    }

    public CloningKind kind() {
        return Kind;
    }

    public CompoundTag sampleOf(ItemStack Stack) {
        return new CompoundTag();
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        if (Kind.creativeOnly()) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.creative_only").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.dclass_blood.use", CloningMachineBlockEntity.CycleTicks / 20).withStyle(ChatFormatting.GOLD));
        }
    }
}
