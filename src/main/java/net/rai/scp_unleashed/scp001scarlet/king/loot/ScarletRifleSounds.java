package net.rai.scp_unleashed.scp001scarlet.king.loot;

import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;

import java.util.List;

public final class ScarletRifleSounds {
    private ScarletRifleSounds() {
    }

    public static List<GunSounds.Layer> shot() {
        return List.of(new GunSounds.Layer(GunsRegistry.ShotPunch, 0.36F, 1.72F, 0, false), new GunSounds.Layer(ScarletKingRegistry.RifleShot, 0.55F, 1.0F, 0, false),
                new GunSounds.Layer(ScarletKingRegistry.RifleCrack, 0.3F, 1.0F, 0, true), new GunSounds.Layer(GunsRegistry.ShotEcho, 0.07F, 1.85F, 1, true));
    }

    public static List<GunSounds.Layer> rechargeStart() {
        return List.of(new GunSounds.Layer(ScarletKingRegistry.RifleEmpty, 0.7F, 1.0F, 0, false), new GunSounds.Layer(GunsRegistry.Fizz, 0.4F, 1.4F, 0, false),
                new GunSounds.Layer(ScarletKingRegistry.RifleRecharge, 0.6F, 1.0F, 8, false));
    }

    public static List<GunSounds.Layer> rechargeFinish() {
        return List.of(new GunSounds.Layer(ScarletKingRegistry.RifleReady, 0.7F, 1.0F, 0, false), new GunSounds.Layer(GunsRegistry.MechanismClick, 0.35F, 1.6F, 1, false));
    }
}
