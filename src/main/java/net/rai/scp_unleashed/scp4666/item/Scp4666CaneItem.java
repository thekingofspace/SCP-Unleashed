package net.rai.scp_unleashed.scp4666.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

public class Scp4666CaneItem extends Scp4666KeepsakeItem {
    public static final double Stride = 0.06;
    public static final double Rap = 3.0;

    public Scp4666CaneItem(Properties Properties) {
        super(Properties.attributes(modifiers()), "scp4666_bone_cane", () -> Scp4666Registry.Knock.get(), 1.4f);
    }

    public static ItemAttributeModifiers modifiers() {
        ResourceLocation Walk = Scp4666Registry.location("scp4666_cane_stride");
        return ItemAttributeModifiers.builder()
                .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(Walk, Stride, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, Rap, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
