package net.rai.scp_unleashed.gear;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.materials.MaterialsRegistry;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public final class GearMaterials {
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);
    public static final TagKey<Item> SteelPlates = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/steel"));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Mtf = register("gear_mtf", 3, 8, 6, 3, 10, SoundEvents.ARMOR_EQUIP_IRON, 2.0F, 0.05F,
            () -> Ingredient.of(SteelPlates));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SiteGuard = register("gear_site_guard", 2, 6, 5, 2, 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F, 0.0F,
            () -> Ingredient.of(Tags.Items.LEATHERS));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Hazmat = register("gear_hazmat", 1, 3, 2, 1, 9, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            () -> Ingredient.of(MaterialsRegistry.InsulationSheet.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GasMask = register("gear_gas_mask", 1, 0, 0, 0, 9, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            () -> Ingredient.of(MaterialsRegistry.Rubber.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DClass = register("gear_d_class", 0, 2, 2, 1, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            () -> Ingredient.of(Items.ORANGE_WOOL));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Goc = register("gear_goc", 3, 8, 6, 3, 10, GearSounds.GocEquip, 1.5F, 0.05F,
            () -> Ingredient.of(SteelPlates));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SerpentsHand = register("gear_serpents_hand", 1, 5, 4, 1, 15, GearSounds.SerpentsHandEquip, 0.0F, 0.0F,
            () -> Ingredient.of(Tags.Items.LEATHERS));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ChaosInsurgency = register("gear_chaos_insurgency", 3, 8, 5, 3, 9, GearSounds.ChaosInsurgencyEquip, 2.0F, 0.05F,
            () -> Ingredient.of(SteelPlates));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SiteGuardTactical = register("gear_site_guard_tactical", 2, 6, 5, 2, 12, GearSounds.SiteGuardTacticalEquip, 0.5F, 0.0F,
            () -> Ingredient.of(Tags.Items.LEATHERS));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Scientist = register("gear_scientist", 1, 5, 3, 1, 15, GearSounds.ScientistEquip, 0.0F, 0.0F,
            () -> Ingredient.of(Items.WHITE_WOOL));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BrokenGod = register("gear_broken_god", 2, 7, 5, 2, 12, GearSounds.BrokenGodEquip, 1.0F, 0.05F,
            () -> Ingredient.of(SteelPlates));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Sarkic = register("gear_sarkic", 2, 5, 4, 1, 18, GearSounds.SarkicEquip, 0.0F, 0.0F,
            () -> Ingredient.of(Items.ROTTEN_FLESH));

    private GearMaterials() {
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(String Name, int Helmet, int Chest, int Legs, int Boots, int Enchantability,
                                                                       Holder<SoundEvent> Sound, float Toughness, float KnockbackResistance, Supplier<Ingredient> Repair) {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        Defense.put(ArmorItem.Type.HELMET, Helmet);
        Defense.put(ArmorItem.Type.CHESTPLATE, Chest);
        Defense.put(ArmorItem.Type.LEGGINGS, Legs);
        Defense.put(ArmorItem.Type.BOOTS, Boots);
        Defense.put(ArmorItem.Type.BODY, Chest);
        List<ArmorMaterial.Layer> Layers = List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
        return MaterialRegister.register(Name, () -> new ArmorMaterial(Defense, Enchantability, Sound, Repair, Layers, Toughness, KnockbackResistance));
    }
}
