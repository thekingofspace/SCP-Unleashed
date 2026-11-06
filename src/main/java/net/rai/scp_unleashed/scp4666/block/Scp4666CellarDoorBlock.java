package net.rai.scp_unleashed.scp4666.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

import java.util.List;

public class Scp4666CellarDoorBlock extends DoorBlock {
    public static final SoundEvent Opening = SoundEvent.createVariableRangeEvent(Scp4666Registry.location("scp4666_cellar_open"));
    public static final SoundEvent Closing = SoundEvent.createVariableRangeEvent(Scp4666Registry.location("scp4666_cellar_close"));
    public static final BlockSetType Cellar = BlockSetType.register(new BlockSetType("scp_unleashed:scp4666_cellar", true, true, false, BlockSetType.PressurePlateSensitivity.EVERYTHING,
            SoundType.WOOD, Closing, Opening, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF,
            SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON, SoundEvents.WOODEN_BUTTON_CLICK_OFF, SoundEvents.WOODEN_BUTTON_CLICK_ON));

    public Scp4666CellarDoorBlock(Properties Properties) {
        super(Cellar, Properties);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_cellar_door.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
