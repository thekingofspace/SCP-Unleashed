package net.rai.scp_unleashed.scp682;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp682RemoverItem extends Item {
    public static final double Range = 64.0;

    public Scp682RemoverItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public boolean isFoil(ItemStack Stack) {
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack Stack, Player User, LivingEntity Target, InteractionHand Hand) {
        if (!(Target instanceof Scp682 Reptile)) {
            return InteractionResult.PASS;
        }
        return remove(User, Reptile) ? InteractionResult.sidedSuccess(User.level().isClientSide) : InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        Scp682 Reptile = aimed(User);
        if (Reptile == null) {
            return InteractionResultHolder.pass(Stack);
        }
        return remove(User, Reptile) ? InteractionResultHolder.sidedSuccess(Stack, World.isClientSide) : InteractionResultHolder.fail(Stack);
    }

    @Nullable
    public static Scp682 aimed(Player User) {
        Vec3 Eye = User.getEyePosition();
        Vec3 Reach = Eye.add(User.getViewVector(1.0f).scale(Range));
        AABB Sweep = User.getBoundingBox().expandTowards(User.getViewVector(1.0f).scale(Range)).inflate(1.0);
        EntityHitResult Hit = ProjectileUtil.getEntityHitResult(User.level(), User, Eye, Reach, Sweep, Found -> Found instanceof Scp682 Reptile && Reptile.isAlive(), 0.3f);
        return Hit != null && Hit.getEntity() instanceof Scp682 Reptile ? Reptile : null;
    }

    public static boolean remove(Player User, Scp682 Reptile) {
        if (!User.getAbilities().instabuild) {
            if (!User.level().isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp682_remover.creative").withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        if (User.level() instanceof ServerLevel World) {
            Reptile.vanish(World);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp682_remover.removed").withStyle(ChatFormatting.DARK_AQUA), true);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp682_remover.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp682_remover.detail.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp682_remover.detail.1").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
