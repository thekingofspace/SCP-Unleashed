package net.rai.scp_unleashed.space;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;

public enum SpaceSuitPiece {
    Helmet("space_suit_helmet", ArmorItem.Type.HELMET),
    Chestplate("space_suit_chestplate", ArmorItem.Type.CHESTPLATE),
    Leggings("space_suit_leggings", ArmorItem.Type.LEGGINGS),
    Boots("space_suit_boots", ArmorItem.Type.BOOTS);

    public static final int DurabilityFactor = 25;

    private final String Id;
    private final ArmorItem.Type ArmorType;
    private final ResourceLocation ModelTexture;

    SpaceSuitPiece(String Id, ArmorItem.Type ArmorType) {
        this.Id = Id;
        this.ArmorType = ArmorType;
        this.ModelTexture = SpaceRegistry.location("textures/models/armor/" + Id + ".png");
    }

    public String getId() {
        return Id;
    }

    public ArmorItem.Type getArmorType() {
        return ArmorType;
    }

    public EquipmentSlot getSlot() {
        return ArmorType.getSlot();
    }

    public ResourceLocation getModelTexture() {
        return ModelTexture;
    }
}
