package net.rai.scp_unleashed.scp020;

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

public class Scp020GogglesItem extends ArmorItem {
    public static final String Key = "item.scp_unleashed.scp020_hyperspectral_goggles";
    public static final ResourceLocation ModelTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/models/armor/scp020_hyperspectral_goggles.png");

    public Scp020GogglesItem(Holder<ArmorMaterial> Material, Properties Settings) {
        super(Material, Type.HELMET, Settings);
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer ArmorLayer, boolean InnerModel) {
        return ModelTexture;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + ".tooltip.details").withStyle(ChatFormatting.GOLD));
            Goggles.addSlotLine(Lines, Goggles.Kind.Hyperspectral);
        }
    }
}
