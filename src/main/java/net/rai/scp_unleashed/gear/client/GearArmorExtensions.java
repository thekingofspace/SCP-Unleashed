package net.rai.scp_unleashed.gear.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.rai.scp_unleashed.gear.GearPiece;

public class GearArmorExtensions implements IClientItemExtensions {
    private final GearPiece Piece;

    public GearArmorExtensions(GearPiece Piece) {
        this.Piece = Piece;
    }

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, HumanoidModel<?> Original) {
        return GearArmorModels.get(Piece);
    }

    @Override
    public void setupModelAnimations(LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, Model Shown, float LimbSwing, float LimbSwingAmount, float PartialTick,
                                     float AgeInTicks, float NetHeadYaw, float HeadPitch) {
        if (Piece.coversLegs() && Slot == EquipmentSlot.CHEST && Shown instanceof HumanoidModel<?> Shape) {
            Shape.rightLeg.visible = true;
            Shape.leftLeg.visible = true;
        }
    }
}
