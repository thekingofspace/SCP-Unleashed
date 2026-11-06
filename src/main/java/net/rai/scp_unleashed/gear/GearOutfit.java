package net.rai.scp_unleashed.gear;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

public enum GearOutfit {
    GasMask("gas_mask", GearMaterials.GasMask, 10, true),
    Hazmat("hazmat", GearMaterials.Hazmat, 12, true),
    Mtf("mtf", GearMaterials.Mtf, 30, true),
    SiteGuard("site_guard", GearMaterials.SiteGuard, 18, true),
    DClass("d_class", GearMaterials.DClass, 6, false),
    Goc("goc", GearMaterials.Goc, 32, true),
    SerpentsHand("serpents_hand", GearMaterials.SerpentsHand, 15, true),
    ChaosInsurgency("chaos_insurgency", GearMaterials.ChaosInsurgency, 28, true),
    SiteGuardTactical("site_guard_tactical", GearMaterials.SiteGuardTactical, 18, true),
    Scientist("scientist", GearMaterials.Scientist, 14, true),
    BrokenGod("broken_god", GearMaterials.BrokenGod, 22, true),
    Sarkic("sarkic", GearMaterials.Sarkic, 16, true);

    private final String Key;
    private final Holder<ArmorMaterial> Armor;
    private final int DurabilityFactor;
    private final boolean CustomModel;

    GearOutfit(String Key, Holder<ArmorMaterial> Armor, int DurabilityFactor, boolean CustomModel) {
        this.Key = Key;
        this.Armor = Armor;
        this.DurabilityFactor = DurabilityFactor;
        this.CustomModel = CustomModel;
    }

    public String getKey() {
        return Key;
    }

    public Holder<ArmorMaterial> getArmor() {
        return Armor;
    }

    public int getDurabilityFactor() {
        return DurabilityFactor;
    }

    public boolean hasCustomModel() {
        return CustomModel;
    }
}
