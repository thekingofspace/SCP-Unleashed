package net.rai.scp_unleashed.scp3008.econ.item;

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
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp3008StoreArmorItem extends ArmorItem {
    public static final double NoticeShare = 0.6;
    @Nullable
    private final ResourceLocation Worn;

    public Scp3008StoreArmorItem(Holder<ArmorMaterial> Material, Type Slot, Properties Settings, @Nullable ResourceLocation Worn) {
        super(Material, Slot, Settings);
        this.Worn = Worn;
    }

    public static boolean fullSet(LivingEntity Wearer) {
        for (EquipmentSlot Slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(Wearer.getItemBySlot(Slot).getItem() instanceof Scp3008StoreArmorItem)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer ArmorLayer, boolean InnerModel) {
        return Worn;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.scp3008_econ_store_set").withStyle(ChatFormatting.GOLD));
        }
    }
}
