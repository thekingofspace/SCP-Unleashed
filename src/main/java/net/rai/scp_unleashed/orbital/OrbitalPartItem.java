package net.rai.scp_unleashed.orbital;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteItem;

import java.util.List;
import java.util.function.IntSupplier;

public class OrbitalPartItem extends SatelliteItem {
    private static IntSupplier KnownMask = () -> -1;

    private final OrbitalPart Part;

    public OrbitalPartItem(OrbitalPart Part, Properties Settings) {
        super(Settings);
        this.Part = Part;
    }

    public OrbitalPart getPart() {
        return Part;
    }

    public static void setKnownMask(IntSupplier Supplier) {
        KnownMask = Supplier;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Part.getTranslationKey() + ".tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("tooltip.scp_unleashed.orbital.part_of").withStyle(ChatFormatting.DARK_AQUA));
        int Mask = KnownMask.getAsInt();
        if (Mask >= 0) {
            boolean Owned = (Mask & Part.bit()) != 0;
            Lines.add(Component.translatable(Owned ? "tooltip.scp_unleashed.orbital.in_orbit" : "tooltip.scp_unleashed.orbital.not_in_orbit",
                    Integer.bitCount(Mask & OrbitalPart.FullMask), OrbitalPart.Count).withStyle(Owned ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.orbital.part_launch").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("tooltip.scp_unleashed.orbital.part_owner").withStyle(ChatFormatting.GOLD));
        }
    }
}
