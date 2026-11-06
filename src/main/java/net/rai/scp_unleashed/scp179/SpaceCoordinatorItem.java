package net.rai.scp_unleashed.scp179;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.archive.UniqueGifts;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpaceCoordinatorItem extends Item {
    public SpaceCoordinatorItem(Properties Properties) {
        super(Properties);
    }

    public static ItemStack create(ResourceLocation Target) {
        ItemStack Stack = new ItemStack(Scp179Registry.Coordinator.get());
        Stack.set(Scp179Registry.CoordinatorTarget.get(), Target);
        return Stack;
    }

    @Nullable
    public static ResourceLocation target(ItemStack Stack) {
        return Stack.get(Scp179Registry.CoordinatorTarget.get());
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.space_coordinator.tooltip").withStyle(ChatFormatting.GRAY));
        ResourceLocation Target = target(Stack);
        SkyTarget Known = SkyTargets.get(Target);
        if (Target == null) {
            Lines.add(Component.translatable("item.scp_unleashed.space_coordinator.blank").withStyle(ChatFormatting.DARK_GRAY));
        } else if (Known != null) {
            Lines.add(Component.translatable("item.scp_unleashed.space_coordinator.target", Known.label().copy().withStyle(ChatFormatting.LIGHT_PURPLE), Known.region())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            Lines.add(Component.translatable("item.scp_unleashed.space_coordinator.unknown", Target.toString()).withStyle(ChatFormatting.GRAY));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.space_coordinator.use").withStyle(ChatFormatting.GOLD));
            Lines.add(UniqueGifts.keeps(null) ? UniqueGifts.keptLine()
                    : Component.translatable("item.scp_unleashed.space_coordinator.spent").withStyle(ChatFormatting.GOLD));
        }
    }
}
