package net.rai.scp_unleashed.scp178;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp178GlassesItem extends ArmorItem {
    public static final ResourceLocation ModelTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/models/armor/scp178_glasses.png");
    public static final String Prefix = "item.scp_unleashed.scp178_glasses.";

    public Scp178GlassesItem(Holder<ArmorMaterial> Material, Properties Settings) {
        super(Material, Type.HELMET, Settings);
    }

    public static boolean isWorn(@Nullable LivingEntity Wearer) {
        return Wearer != null && Wearer.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof Scp178GlassesItem;
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer ArmorLayer, boolean InnerModel) {
        return ModelTexture;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack Stack) {
        return ItemAttributeModifiers.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "tooltip.details").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "tooltip.warning").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
