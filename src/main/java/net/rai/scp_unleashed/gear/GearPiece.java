package net.rai.scp_unleashed.gear;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.rai.scp_unleashed.Scp_unleashed;

public enum GearPiece {
    GasMask("gas_mask", GearOutfit.GasMask, ArmorItem.Type.HELMET),
    HazmatHood("hazmat_hood", GearOutfit.Hazmat, ArmorItem.Type.HELMET),
    HazmatSuit("hazmat_suit", GearOutfit.Hazmat, ArmorItem.Type.CHESTPLATE),
    HazmatTrousers("hazmat_trousers", GearOutfit.Hazmat, ArmorItem.Type.LEGGINGS),
    HazmatBoots("hazmat_boots", GearOutfit.Hazmat, ArmorItem.Type.BOOTS),
    MtfHelmet("mtf_helmet", GearOutfit.Mtf, ArmorItem.Type.HELMET),
    MtfVest("mtf_vest", GearOutfit.Mtf, ArmorItem.Type.CHESTPLATE),
    MtfPants("mtf_pants", GearOutfit.Mtf, ArmorItem.Type.LEGGINGS),
    MtfBoots("mtf_boots", GearOutfit.Mtf, ArmorItem.Type.BOOTS),
    SiteGuardCap("site_guard_cap", GearOutfit.SiteGuard, ArmorItem.Type.HELMET),
    SiteGuardShirt("site_guard_shirt", GearOutfit.SiteGuard, ArmorItem.Type.CHESTPLATE),
    SiteGuardTrousers("site_guard_trousers", GearOutfit.SiteGuard, ArmorItem.Type.LEGGINGS),
    SiteGuardBoots("site_guard_boots", GearOutfit.SiteGuard, ArmorItem.Type.BOOTS),
    DClassJumpsuit("d_class_jumpsuit", GearOutfit.DClass, ArmorItem.Type.CHESTPLATE),
    DClassTrousers("d_class_trousers", GearOutfit.DClass, ArmorItem.Type.LEGGINGS),
    DClassShoes("d_class_shoes", GearOutfit.DClass, ArmorItem.Type.BOOTS),
    GocHelmet("goc_helmet", GearOutfit.Goc, ArmorItem.Type.HELMET),
    GocArmor("goc_armor", GearOutfit.Goc, ArmorItem.Type.CHESTPLATE),
    GocTrousers("goc_trousers", GearOutfit.Goc, ArmorItem.Type.LEGGINGS),
    GocBoots("goc_boots", GearOutfit.Goc, ArmorItem.Type.BOOTS),
    SerpentsHandHood("serpents_hand_hood", GearOutfit.SerpentsHand, ArmorItem.Type.HELMET),
    SerpentsHandJacket("serpents_hand_jacket", GearOutfit.SerpentsHand, ArmorItem.Type.CHESTPLATE),
    SerpentsHandTrousers("serpents_hand_trousers", GearOutfit.SerpentsHand, ArmorItem.Type.LEGGINGS),
    SerpentsHandBoots("serpents_hand_boots", GearOutfit.SerpentsHand, ArmorItem.Type.BOOTS),
    ChaosInsurgencyHelmet("chaos_insurgency_helmet", GearOutfit.ChaosInsurgency, ArmorItem.Type.HELMET),
    ChaosInsurgencyVest("chaos_insurgency_vest", GearOutfit.ChaosInsurgency, ArmorItem.Type.CHESTPLATE),
    ChaosInsurgencyPants("chaos_insurgency_pants", GearOutfit.ChaosInsurgency, ArmorItem.Type.LEGGINGS),
    ChaosInsurgencyBoots("chaos_insurgency_boots", GearOutfit.ChaosInsurgency, ArmorItem.Type.BOOTS),
    SiteGuardTacticalHelmet("site_guard_tactical_helmet", GearOutfit.SiteGuardTactical, ArmorItem.Type.HELMET),
    SiteGuardTacticalUniform("site_guard_tactical_uniform", GearOutfit.SiteGuardTactical, ArmorItem.Type.CHESTPLATE),
    SiteGuardTacticalTrousers("site_guard_tactical_trousers", GearOutfit.SiteGuardTactical, ArmorItem.Type.LEGGINGS),
    SiteGuardTacticalBoots("site_guard_tactical_boots", GearOutfit.SiteGuardTactical, ArmorItem.Type.BOOTS),
    ScientistGlasses("scientist_glasses", GearOutfit.Scientist, ArmorItem.Type.HELMET),
    ScientistLabCoat("scientist_lab_coat", GearOutfit.Scientist, ArmorItem.Type.CHESTPLATE),
    ScientistSlacks("scientist_slacks", GearOutfit.Scientist, ArmorItem.Type.LEGGINGS),
    ScientistShoes("scientist_shoes", GearOutfit.Scientist, ArmorItem.Type.BOOTS),
    BrokenGodMask("broken_god_mask", GearOutfit.BrokenGod, ArmorItem.Type.HELMET),
    BrokenGodRobe("broken_god_robe", GearOutfit.BrokenGod, ArmorItem.Type.CHESTPLATE),
    BrokenGodGreaves("broken_god_greaves", GearOutfit.BrokenGod, ArmorItem.Type.LEGGINGS),
    BrokenGodBoots("broken_god_boots", GearOutfit.BrokenGod, ArmorItem.Type.BOOTS),
    SarkicHood("sarkic_hood", GearOutfit.Sarkic, ArmorItem.Type.HELMET),
    SarkicRobe("sarkic_robe", GearOutfit.Sarkic, ArmorItem.Type.CHESTPLATE),
    SarkicLeggings("sarkic_leggings", GearOutfit.Sarkic, ArmorItem.Type.LEGGINGS),
    SarkicBoots("sarkic_boots", GearOutfit.Sarkic, ArmorItem.Type.BOOTS);

    private final String Id;
    private final GearOutfit Outfit;
    private final ArmorItem.Type ArmorType;
    private final ResourceLocation ModelTexture;

    GearPiece(String Id, GearOutfit Outfit, ArmorItem.Type ArmorType) {
        this.Id = Id;
        this.Outfit = Outfit;
        this.ArmorType = ArmorType;
        this.ModelTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/models/armor/gear_" + Id + ".png");
    }

    public String getId() {
        return Id;
    }

    public GearOutfit getOutfit() {
        return Outfit;
    }

    public ArmorItem.Type getArmorType() {
        return ArmorType;
    }

    public boolean hasCustomModel() {
        return Outfit.hasCustomModel();
    }

    public ResourceLocation getModelTexture() {
        return ModelTexture;
    }

    public int getDurability() {
        return ArmorType.getDurability(Outfit.getDurabilityFactor());
    }

    public boolean coversLegs() {
        return this == ScientistLabCoat || this == BrokenGodRobe || this == SarkicRobe;
    }
}
