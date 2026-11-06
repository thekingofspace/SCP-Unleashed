package net.rai.scp_unleashed.mixin.scp268;

import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.rai.scp_unleashed.scp268.Scp268;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class Scp268LivingEntityMixin {
    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void scp268Unseen(CallbackInfoReturnable<Boolean> Info) {
        if (Scp268.isNeglected((LivingEntity) (Object) this)) {
            Info.setReturnValue(false);
        }
    }

    @Inject(method = "take", at = @At("HEAD"), cancellable = true)
    private void scp268QuietPickup(Entity Taken, int Amount, CallbackInfo Info) {
        Object Self = this;
        if (!(Self instanceof ServerPlayer User) || !Scp268.isNeglected(User) || Taken.isRemoved()) {
            return;
        }
        if (Taken instanceof ItemEntity || Taken instanceof AbstractArrow || Taken instanceof ExperienceOrb) {
            User.connection.send(new ClientboundTakeItemEntityPacket(Taken.getId(), User.getId(), Amount));
        }
        Info.cancel();
    }
}
