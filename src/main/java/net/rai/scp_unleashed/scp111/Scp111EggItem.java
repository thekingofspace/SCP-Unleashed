package net.rai.scp_unleashed.scp111;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp111EggItem extends BlockItem {
    public static final int StackSize = 16;
    public static final String Key = "item.scp_unleashed.scp111_egg";

    public Scp111EggItem(Block Placed, Properties Properties) {
        super(Placed, Properties);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp111EggItem;
    }

    public static boolean isShelf(ItemStack Stack) {
        return is(Stack) && Stack.has(Scp111Registry.ClutchData.get());
    }

    @Override
    public String getDescriptionId() {
        return Key;
    }

    @Override
    public Component getName(ItemStack Stack) {
        if (isShelf(Stack)) {
            return Component.translatable(Key + ".shelf", Scp111Clutch.of(Stack).size());
        }
        return super.getName(Stack);
    }

    @Override
    public InteractionResult place(BlockPlaceContext Context) {
        if (isShelf(Context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        return super.place(Context);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        if (isShelf(Stack)) {
            Lines.add(Component.translatable(Key + ".shelf.tooltip").withStyle(ChatFormatting.GRAY));
            return;
        }
        Scp111Egg Egg = Scp111Egg.of(Stack);
        Lines.add(Egg.Variant().displayName());
        Lines.add(Component.translatable(Key + ".stage." + Egg.Stage()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + ".details.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + ".details.1").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + ".details.2").withStyle(ChatFormatting.GOLD));
        }
    }
}
