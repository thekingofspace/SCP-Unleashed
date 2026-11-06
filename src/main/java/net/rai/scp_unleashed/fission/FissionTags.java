package net.rai.scp_unleashed.fission;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.rai.scp_unleashed.Scp_unleashed;

public final class FissionTags {
    public static final TagKey<Item> FissionFuel = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fission_fuel"));
    public static final TagKey<Item> ToxicGasProtection = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "toxic_gas_protection"));
    public static final TagKey<Fluid> WasteWater = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "waste_water"));

    private FissionTags() {
    }

    public static boolean isProtected(LivingEntity Target) {
        return Target.getItemBySlot(EquipmentSlot.HEAD).is(ToxicGasProtection);
    }
}
