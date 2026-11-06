package net.rai.scp_unleashed.mixin.scp268;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.scp268.Scp268Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
public abstract class Scp268ArmorSlotMixin {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void scp268HoldCap(Player User, CallbackInfoReturnable<Boolean> Info) {
        if (((Slot) (Object) this).getItem().is(Scp268Registry.Cap.get()) && Scp268.settling(User)) {
            Info.setReturnValue(false);
        }
    }
}
