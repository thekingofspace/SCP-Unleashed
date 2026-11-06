package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;

public final class PlayerGunCarry {
    private PlayerGunCarry() {
    }

    public static float share(LivingEntity Holder, ItemStack Stack, float Partial) {
        if (!(Holder instanceof Player)) {
            return 0.0F;
        }
        GunKind Kind = GunItem.kindOf(Stack);
        if (Kind == null || Kind.getStats().isLongGun()) {
            return 0.0F;
        }
        if (Stack == Holder.getMainHandItem()) {
            return 1.0F - GunStance.peek(Holder, Partial);
        }
        GunKind Main = GunItem.kindOf(Holder.getMainHandItem());
        if (Main == null) {
            return 1.0F;
        }
        return Main.getStats().isLongGun() || GunStance.peek(Holder, Partial) >= 1.0F ? 0.0F : 1.0F;
    }

    public static void straighten(LivingEntity Holder, ItemStack Stack, ItemDisplayContext Context, boolean Left, PoseStack Pose) {
        if (Context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && Context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return;
        }
        GunKind Kind = GunItem.kindOf(Stack);
        if (Kind != null && Kind.getStats().isHeavy() && Holder instanceof Player && Stack == Holder.getMainHandItem()) {
            GunStance.tilt(Pose, 1.0F - GunStance.peek(Holder, GunStance.partial()));
            return;
        }
        GunStance.straighten(Pose, Holder, Stack, Left, share(Holder, Stack, GunStance.partial()));
    }
}
