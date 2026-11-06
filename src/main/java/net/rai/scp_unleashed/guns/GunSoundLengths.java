package net.rai.scp_unleashed.guns;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;
import java.util.Map;

public final class GunSoundLengths {
    public static final float UnknownSeconds = 0.5F;
    public static final float SlowestPitch = 0.5F;
    private static final Map<ResourceLocation, Float> Seconds = new HashMap<>();

    static {
        put(GunsRegistry.ShotCrack, 1.0F);
        put(GunsRegistry.ShotPunch, 0.5F);
        put(GunsRegistry.ShotWhump, 1.96F);
        put(GunsRegistry.ShotHeavy, 2.0F);
        put(GunsRegistry.ShotBoom, 3.25F);
        put(GunsRegistry.ShotEcho, 2.5F);
        put(GunsRegistry.ShotThunder, 7.68F);
        put(GunsRegistry.Pneumatic, 0.31F);
        put(GunsRegistry.PneumaticVent, 0.55F);
        put(GunsRegistry.MechanismClick, 0.28F);
        put(GunsRegistry.MechanismClunk, 0.55F);
        put(GunsRegistry.MechanismOpen, 0.2F);
        put(GunsRegistry.MechanismClose, 0.21F);
        put(GunsRegistry.MechanismChain, 0.42F);
        put(GunsRegistry.MechanismDoor, 0.42F);
        put(GunsRegistry.MechanismLoad, 1.07F);
        put(GunsRegistry.DisruptorCharge, 1.63F);
        put(GunsRegistry.DisruptorPower, 2.95F);
        put(GunsRegistry.DisruptorWhine, 1.02F);
        put(GunsRegistry.DisruptorDischarge, 4.0F);
        put(GunsRegistry.DisruptorFade, 3.5F);
        put(GunsRegistry.DisruptorBurnout, 2.85F);
        put(GunsRegistry.Fizz, 1.25F);
        put(GunsRegistry.HitMarker, 0.75F);
        put(GunsRegistry.BenchAnvil, 1.25F);
        put(GunsRegistry.BenchSmithing, 0.73F);
    }

    private GunSoundLengths() {
    }

    private static void put(DeferredHolder<SoundEvent, SoundEvent> Sound, float Length) {
        Seconds.put(Sound.getId(), Length);
    }

    public static boolean known(ResourceLocation Id) {
        return Seconds.containsKey(Id);
    }

    public static float seconds(ResourceLocation Id) {
        return Seconds.getOrDefault(Id, UnknownSeconds);
    }

    public static int ticks(ResourceLocation Id, float Pitch) {
        return Math.max(1, Mth.ceil(seconds(Id) * 20.0F / Math.max(SlowestPitch, Pitch)));
    }
}
