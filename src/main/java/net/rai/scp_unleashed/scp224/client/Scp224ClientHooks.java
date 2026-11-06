package net.rai.scp_unleashed.scp224.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp224.Scp224ClockBlockEntity;
import net.rai.scp_unleashed.scp224.Scp224Motion;
import net.rai.scp_unleashed.scp224.Scp224Registry;
import net.rai.scp_unleashed.scp224.network.Scp224AgePayload;

public final class Scp224ClientHooks {
    public static final float TickVolume = 0.32f;
    public static final float TickPitch = 1.0f;
    public static final float TockPitch = 0.84f;
    public static final double TickHeight = 15.0 / 16.0;

    private Scp224ClientHooks() {
    }

    public static void age(Scp224AgePayload Payload) {
        Scp224ClientState.apply(Payload);
    }

    public static void clockTick(Level World, BlockPos Pos, Scp224ClockBlockEntity Clock) {
        if (Clock.isStilled()) {
            return;
        }
        long Time = World.getGameTime();
        if (!Scp224Motion.isBeat(Time)) {
            return;
        }
        LocalPlayer User = Minecraft.getInstance().player;
        Vec3 At = new Vec3(Pos.getX() + 0.5, Pos.getY() + TickHeight, Pos.getZ() + 0.5);
        if (User == null || User.position().distanceToSqr(At) > (double) Scp224Registry.TickRange * Scp224Registry.TickRange) {
            return;
        }
        float Pitch = Scp224Motion.isTock(Time) ? TockPitch : TickPitch;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp224Registry.TickLayers) {
            World.playLocalSound(At.x, At.y, At.z, Layer.get(), SoundSource.BLOCKS, TickVolume, Pitch, false);
        }
    }
}
