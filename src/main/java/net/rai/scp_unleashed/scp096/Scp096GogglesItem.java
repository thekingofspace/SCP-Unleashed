package net.rai.scp_unleashed.scp096;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.goggles.Goggles;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp096GogglesItem extends ArmorItem {
    public static final ResourceLocation ModelTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/models/armor/scramble_goggles.png");

    public Scp096GogglesItem(Holder<ArmorMaterial> Material, Properties Properties) {
        super(Material, Type.HELMET, Properties);
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer ArmorLayer, boolean InnerModel) {
        return ModelTexture;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scramble_goggles.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scramble_goggles.tooltip.details").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scramble_goggles.tooltip.warning").withStyle(ChatFormatting.DARK_RED));
            Goggles.addSlotLine(Lines, Goggles.Kind.Scramble);
        }
    }
}
