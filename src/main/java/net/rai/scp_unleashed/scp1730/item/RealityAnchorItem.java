package net.rai.scp_unleashed.scp1730.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.rai.scp_unleashed.grenade.GrenadeRegistry;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp1730.entity.RealityAnchorEntity;

import java.util.List;

public class RealityAnchorItem extends Item implements ProjectileItem {
    public static final int UseTicks = 72000;
    public static final int FullChargeTicks = 16;
    public static final float MinSpeed = 0.45f;
    public static final float MaxSpeed = 1.15f;
    public static final int CooldownTicks = 20;

    public RealityAnchorItem(Properties Props) {
        super(Props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        User.startUsingItem(Hand);
        if (!World.isClientSide) {
            World.playSound(null, User.getX(), User.getY(), User.getZ(), GrenadeRegistry.PinPull.get(), SoundSource.PLAYERS, 0.8f, 0.8f);
        }
        return InteractionResultHolder.consume(User.getItemInHand(Hand));
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
        float Charge = Mth.clamp((getUseDuration(Stack, User) - TimeLeft) / (float) FullChargeTicks, 0.0f, 1.0f);
        if (World instanceof ServerLevel Server) {
            RealityAnchorEntity.thrown(Server, User, Mth.lerp(Charge, MinSpeed, MaxSpeed));
            Server.playSound(null, User.getX(), User.getY(), User.getZ(), GrenadeRegistry.Throw.get(), SoundSource.PLAYERS, 0.8f, 0.9f);
        }
        User.swing(User.getUsedItemHand());
        if (User instanceof Player Thrower) {
            Thrower.awardStat(Stats.ITEM_USED.get(this));
            Thrower.getCooldowns().addCooldown(this, CooldownTicks);
        }
        Stack.consume(1, User);
    }

    @Override
    public Projectile asProjectile(Level World, Position Pos, ItemStack Stack, Direction Facing) {
        return RealityAnchorEntity.dispensed(World, Pos.x(), Pos.y(), Pos.z());
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
