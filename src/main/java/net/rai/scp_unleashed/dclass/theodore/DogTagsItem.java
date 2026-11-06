package net.rai.scp_unleashed.dclass.theodore;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DogTagsItem extends Item {
    public DogTagsItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.theodore_dog_tags.tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("item.scp_unleashed.theodore_dog_tags.count", Theodore.ClusterSize).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
