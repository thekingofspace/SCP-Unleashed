package net.rai.scp_unleashed.mixin.upgrade;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.rai.scp_unleashed.upgrade.client.MufflerClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class SoundPacketMufflerMixin {
    @Inject(method = "handleSoundEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V"))
    private void mufflerServerSoundStart(ClientboundSoundPacket Packet, CallbackInfo Info) {
        MufflerClient.serverSound(true);
    }

    @Inject(method = "handleSoundEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", shift = At.Shift.AFTER))
    private void mufflerServerSoundEnd(ClientboundSoundPacket Packet, CallbackInfo Info) {
        MufflerClient.serverSound(false);
    }
}
