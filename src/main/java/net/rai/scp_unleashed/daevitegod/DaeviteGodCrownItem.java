package net.rai.scp_unleashed.daevitegod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DaeviteGodCrownItem extends ArmorItem {
    public static final ResourceLocation ModelTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/models/armor/daevite_god_crown.png");
    public static final int Lifespan = 72000;

    public DaeviteGodCrownItem(Holder<ArmorMaterial> Material, Properties Properties) {
        super(Material, Type.HELMET, Properties);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof DaeviteGodCrownItem;
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer ArmorLayer, boolean InnerModel) {
        return ModelTexture;
    }

    @Override
    public boolean makesPiglinsNeutral(ItemStack Stack, LivingEntity Wearer) {
        return true;
    }

    @Override
    public int getEntityLifespan(ItemStack Stack, Level World) {
        return Lifespan;
    }

    @Override
    public boolean canBeHurtBy(ItemStack Stack, DamageSource Source) {
        return !Source.is(DamageTypeTags.IS_EXPLOSION) && super.canBeHurtBy(Stack, Source);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.daevite_god_crown.tooltip").withStyle(ChatFormatting.GOLD));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.daevite_god_crown.tooltip.details").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.daevite_god_crown.tooltip.warning").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }
}
