package net.rai.scp_unleashed.scp3008.econ.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;

import java.util.List;

public class Scp3008HammerItem extends Scp3008StoreItem {
    public static final int Uses = 220;
    public static final double Damage = 4.0;
    public static final double Speed = -2.8;
    public static final float FurnitureSpeed = 8.0f;
    public static final float GeneralSpeed = 4.0f;
    public static final int Enchantability = 12;
    public static final TagKey<Block> Furniture = TagKey.create(Registries.BLOCK, Scp3008EconRegistry.location("scp3008_liftable"));

    public Scp3008HammerItem(Properties Settings) {
        super(Settings);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, Damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, Speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static Tool createTool() {
        return new Tool(List.of(Tool.Rule.overrideSpeed(Furniture, FurnitureSpeed), Tool.Rule.overrideSpeed(BlockTags.MINEABLE_WITH_AXE, GeneralSpeed),
                Tool.Rule.overrideSpeed(BlockTags.MINEABLE_WITH_PICKAXE, GeneralSpeed)), 1.0f, 1);
    }

    @Override
    public boolean hurtEnemy(ItemStack Stack, LivingEntity Target, LivingEntity Attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack Stack, LivingEntity Target, LivingEntity Attacker) {
        Stack.hurtAndBreak(1, Attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public boolean isValidRepairItem(ItemStack Stack, ItemStack Repair) {
        return Repair.is(Scp3008EconRegistry.SteelTube.get());
    }

    @Override
    public int getEnchantmentValue() {
        return Enchantability;
    }
}
