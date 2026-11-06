package net.rai.scp_unleashed.grenade;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class GrenadeItem extends Item implements ProjectileItem {
    public static final int UseTicks = 72000;
    private final GrenadeKind Kind;

    public GrenadeItem(GrenadeKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
    }

    public GrenadeKind getKind() {
        return Kind;
    }

    public static float charge(int HeldTicks) {
        return Mth.clamp(HeldTicks / (float) GrenadeTuning.FullChargeTicks, 0.0F, 1.0F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        User.startUsingItem(Hand);
        if (!World.isClientSide) {
            GrenadeSounds.pinPull(World, User);
        }
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return UseTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public void releaseUsing(ItemStack Stack, Level World, LivingEntity User, int TimeLeft) {
        float Charge = charge(getUseDuration(Stack, User) - TimeLeft);
        InteractionHand Hand = User.getUsedItemHand();
        if (World instanceof ServerLevel Server) {
            GrenadeEntity.thrown(Server, User, Kind, Charge);
            GrenadeSounds.thrown(Server, User, Charge);
        }
        User.swing(Hand);
        if (User instanceof Player Thrower) {
            Thrower.awardStat(Stats.ITEM_USED.get(this));
            for (GrenadeKind Each : GrenadeKind.values()) {
                Thrower.getCooldowns().addCooldown(GrenadeRegistry.grenadeItem(Each), GrenadeTuning.CooldownTicks);
            }
        }
        Stack.consume(1, User);
    }

    @Override
    public Projectile asProjectile(Level World, Position Pos, ItemStack Stack, Direction Facing) {
        return GrenadeEntity.dispensed(World, Pos.x(), Pos.y(), Pos.z(), Kind);
    }

    @Override
    public DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder().power(GrenadeTuning.DispenseSpeed).uncertainty(GrenadeTuning.DispenseSpread).build();
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(getDescriptionId() + ".detail").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("tooltip.scp_unleashed.grenade.throw").withStyle(ChatFormatting.GRAY));
        }
    }
}
