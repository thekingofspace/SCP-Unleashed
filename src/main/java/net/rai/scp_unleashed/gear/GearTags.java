package net.rai.scp_unleashed.gear;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.rai.scp_unleashed.Scp_unleashed;

public final class GearTags {
    public static final TagKey<Item> ToxicGasProtection = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "toxic_gas_protection"));
    public static final TagKey<Item> HazmatSuit = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "hazmat_suit"));
    private static final EquipmentSlot[] ArmorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private GearTags() {
    }

    public static boolean protectsFromToxicGas(LivingEntity Wearer) {
        return Wearer.getItemBySlot(EquipmentSlot.HEAD).is(ToxicGasProtection);
    }

    public static boolean wearsFullHazmatSuit(LivingEntity Wearer) {
        for (EquipmentSlot Slot : ArmorSlots) {
            if (!Wearer.getItemBySlot(Slot).is(HazmatSuit)) {
                return false;
            }
        }
        return true;
    }
}
