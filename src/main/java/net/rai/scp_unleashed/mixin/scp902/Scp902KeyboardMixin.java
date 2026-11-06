package net.rai.scp_unleashed.mixin.scp902;

import net.minecraft.client.KeyboardHandler;
import net.rai.scp_unleashed.scp902.client.Scp902ClientGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class Scp902KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void scp902HoldKeys(long Window, int Key, int Scan, int Action, int Modifiers, CallbackInfo Info) {
        if (Scp902ClientGate.blocksKey(Window, Key, Scan, Action)) {
            Info.cancel();
        }
    }
}
