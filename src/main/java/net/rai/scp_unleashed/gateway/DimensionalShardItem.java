package net.rai.scp_unleashed.gateway;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class DimensionalShardItem extends Item {
    public static final String Prefix = "item.scp_unleashed.dimensional_shard.";

    public DimensionalShardItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public Component getName(ItemStack Stack) {
        GatewayDestination Destination = GatewayDestinations.of(Stack);
        if (Destination == null) {
            return Component.translatable(Prefix + "blank");
        }
        return Component.translatable(Prefix + "attuned", Destination.displayName());
    }

    @Override
    public boolean isFoil(ItemStack Stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        GatewayDestination Destination = GatewayDestinations.of(Stack);
        Lines.add(Component.translatable(Destination == null ? Prefix + "tooltip_blank" : Prefix + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "use").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "power").withStyle(ChatFormatting.GOLD));
            if (Destination != null) {
                Lines.add(Component.translatable(Prefix + "warning").withStyle(ChatFormatting.RED));
            }
        }
    }

    public static float variantOf(ItemStack Stack) {
        GatewayDestination Destination = GatewayDestinations.of(Stack);
        return Destination == null ? 0.0f : Destination.ShardVariant();
    }
}
