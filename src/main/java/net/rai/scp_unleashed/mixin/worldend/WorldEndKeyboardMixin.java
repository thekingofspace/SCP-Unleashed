package net.rai.scp_unleashed.mixin.worldend;

import net.minecraft.client.KeyboardHandler;
import net.rai.scp_unleashed.worldend.client.WorldEndGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class WorldEndKeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void scpUnleashedWorldEndHoldKeys(long Window, int Key, int Scan, int Action, int Modifiers, CallbackInfo Info) {
        if (WorldEndGate.blocksKey(Window, Key, Scan, Action)) {
            Info.cancel();
        }
    }
}
