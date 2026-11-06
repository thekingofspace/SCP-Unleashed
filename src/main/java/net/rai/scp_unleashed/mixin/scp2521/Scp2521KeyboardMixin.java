package net.rai.scp_unleashed.mixin.scp2521;

import net.minecraft.client.KeyboardHandler;
import net.rai.scp_unleashed.scp2521.client.Scp2521ClientGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class Scp2521KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void scp2521HoldKeys(long Window, int Key, int Scan, int Action, int Modifiers, CallbackInfo Info) {
        if (Scp2521ClientGate.blocksKey(Window, Key, Scan, Action)) {
            Info.cancel();
        }
    }
}
