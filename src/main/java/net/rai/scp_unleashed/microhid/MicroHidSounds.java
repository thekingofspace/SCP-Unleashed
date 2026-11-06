package net.rai.scp_unleashed.microhid;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunsRegistry;

import java.util.List;

public final class MicroHidSounds {
    public static final List<GunSounds.Layer> Empty = List.of(
            new GunSounds.Layer(GunsRegistry.MechanismClick, 0.7F, 1.7F, 0, false),
            new GunSounds.Layer(MicroHidRegistry.Sputter, 0.8F, 1.0F, 0, false),
            new GunSounds.Layer(GunsRegistry.Fizz, 0.3F, 1.5F, 1, false));
    public static final List<GunSounds.Layer> Dock = List.of(
            new GunSounds.Layer(MicroHidRegistry.Dock, 0.8F, 1.15F, 0, false),
            new GunSounds.Layer(MicroHidRegistry.SpinPower, 0.3F, 1.9F, 3, false));
    public static final List<GunSounds.Layer> Undock = List.of(
            new GunSounds.Layer(MicroHidRegistry.Undock, 0.8F, 1.2F, 0, false),
            new GunSounds.Layer(MicroHidRegistry.WindDown, 0.25F, 2.0F, 1, false));
    public static final List<GunSounds.Layer> Full = List.of(
            new GunSounds.Layer(MicroHidRegistry.PlatformFull, 0.7F, 1.6F, 0, false),
            new GunSounds.Layer(MicroHidRegistry.Ring, 0.4F, 1.8F, 3, false));

    private MicroHidSounds() {
    }

    public static void play(Level World, Vec3 At, List<GunSounds.Layer> Layers) {
        GunSounds.play(World, null, At.x, At.y, At.z, Layers, 1.0F);
    }
}
