package net.rai.scp_unleashed.mixin.scp682;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp682.Scp682Kill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(KillCommand.class)
public abstract class Scp682KillCommandMixin {
    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private static void scp682SpareReptile(CommandSourceStack Source, Collection<? extends Entity> Targets, CallbackInfoReturnable<Integer> Callback) {
        if (Scp682Kill.involves(Targets)) {
            Callback.setReturnValue(Scp682Kill.handle(Source, Targets));
        }
    }
}
