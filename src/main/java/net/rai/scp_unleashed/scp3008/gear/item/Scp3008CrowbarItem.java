package net.rai.scp_unleashed.scp3008.gear.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carry;

import java.util.List;

public class Scp3008CrowbarItem extends Item {
    public static final int Uses = 280;
    public static final double Damage = 5.0;
    public static final double Speed = -2.6;
    public static final int LiftWear = 1;
    public static final int Enchantability = 14;

    public Scp3008CrowbarItem(Properties Settings) {
        super(Settings);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, Damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, Speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack Stack, UseOnContext Context) {
        Player User = Context.getPlayer();
        if (User == null || Scp3008Carry.carrying(User)) {
            return InteractionResult.PASS;
        }
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        if (!Scp3008Carry.liftable(World.getBlockState(Pos))) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Scp3008Carry.Outcome Result = Scp3008Carry.lift(User, Pos);
        if (Result == Scp3008Carry.Outcome.Lifted) {
            Stack.hurtAndBreak(LiftWear, User, LivingEntity.getSlotForHand(Context.getHand()));
            return InteractionResult.SUCCESS;
        }
        return Result == Scp3008Carry.Outcome.Ignored ? InteractionResult.PASS : InteractionResult.FAIL;
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
    public boolean mineBlock(ItemStack Stack, Level World, BlockState State, BlockPos Pos, LivingEntity Miner) {
        if (!World.isClientSide && State.getDestroySpeed(World, Pos) != 0.0f) {
            Stack.hurtAndBreak(2, Miner, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public boolean isValidRepairItem(ItemStack Stack, ItemStack Repair) {
        return Repair.is(Scp3008EconRegistry.SteelTube.get());
    }

    @Override
    public int getEnchantmentValue() {
        return Enchantability;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(getDescriptionId() + ".details").withStyle(ChatFormatting.GOLD));
        }
    }
}
