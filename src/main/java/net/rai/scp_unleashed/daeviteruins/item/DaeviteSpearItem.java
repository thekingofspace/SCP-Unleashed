package net.rai.scp_unleashed.daeviteruins.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;
import net.rai.scp_unleashed.daeviteruins.entity.DaeviteThrownSpear;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class DaeviteSpearItem extends Item {
    public static final int Uses = 90;
    public static final double Damage = 4.0;
    public static final double Speed = -2.8;
    public static final double Reach = 1.0;
    public static final int Charge = 10;
    public static final float ThrowSpeed = 2.2f;
    public static final ResourceLocation ReachId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_relic_spear_reach");

    public DaeviteSpearItem(Properties Settings) {
        super(Settings);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, Damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, Speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ReachId, Reach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public boolean canAttackBlock(BlockState State, Level World, BlockPos Pos, Player User) {
        return !User.isCreative();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return 72000;
    }

    private static boolean worn(ItemStack Stack) {
        return Stack.getDamageValue() >= Stack.getMaxDamage() - 1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (worn(Stack)) {
            return InteractionResultHolder.fail(Stack);
        }
        User.startUsingItem(Hand);
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public void releaseUsing(ItemStack Stack, Level World, LivingEntity User, int TimeLeft) {
        if (!(User instanceof Player Thrower) || getUseDuration(Stack, User) - TimeLeft < Charge || worn(Stack)) {
            return;
        }
        if (!World.isClientSide) {
            Stack.hurtAndBreak(1, Thrower, LivingEntity.getSlotForHand(User.getUsedItemHand()));
            DaeviteThrownSpear Spear = new DaeviteThrownSpear(World, Thrower, Stack);
            Spear.shootFromRotation(Thrower, Thrower.getXRot(), Thrower.getYRot(), 0.0f, ThrowSpeed, 1.0f);
            if (Thrower.hasInfiniteMaterials()) {
                Spear.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            World.addFreshEntity(Spear);
            World.playSound(null, Spear, DaeviteRuinsRegistry.SpearThrow.get(), SoundSource.PLAYERS, 1.0f, 0.9f);
            if (!Thrower.hasInfiniteMaterials()) {
                Thrower.getInventory().removeItem(Stack);
            }
        }
        Thrower.awardStat(Stats.ITEM_USED.get(this));
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
    public boolean isRepairable(ItemStack Stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(getDescriptionId() + ".details").withStyle(ChatFormatting.GOLD));
        }
    }
}
