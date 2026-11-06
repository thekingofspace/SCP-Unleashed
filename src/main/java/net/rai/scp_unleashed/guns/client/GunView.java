package net.rai.scp_unleashed.guns.client;

import net.rai.scp_unleashed.guns.GunKind;

public record GunView(float SightY, float RearZ, float MuzzleY, float MuzzleZ, float HipX, float HipY, float HipZ, float HipYaw, float HipPitch, float HipRoll,
                      float AimDepth, float KickBack, float KickPitch, float FlashSize, float AimSpeed) {
    public static GunView of(GunKind Kind) {
        return switch (Kind) {
            case Pistol -> new GunView(14.0F, 16.0F, 10.5F, 0.0F, 0.26F, -0.124F, -0.5F, 4.0F, 1.5F, 0.0F, 0.4F, 0.045F, 7.0F, 0.16F, 0.65F);
            case Revolver -> new GunView(15.0F, 16.0F, 11.0F, -1.0F, 0.26F, -0.124F, -0.5F, 4.0F, 1.5F, 0.0F, 0.4F, 0.06F, 11.0F, 0.2F, 0.65F);
            case AssaultRifle -> new GunView(16.0F, 23.0F, 10.0F, -6.0F, 0.27F, -0.13F, -0.42F, 4.0F, 1.0F, 0.0F, 0.24F, 0.03F, 3.0F, 0.2F, 0.5F);
            case Shotgun -> new GunView(15.0F, 21.0F, 11.0F, -10.0F, 0.27F, -0.13F, -0.42F, 4.0F, 1.0F, 0.0F, 0.26F, 0.07F, 9.0F, 0.26F, 0.5F);
            case SniperRifle -> new GunView(14.5F, 23.0F, 10.0F, -14.0F, 0.27F, -0.14F, -0.42F, 3.5F, 1.0F, 0.0F, 0.14F, 0.08F, 8.0F, 0.24F, 0.35F);
            case ParticleDisruptor -> new GunView(16.0F, 26.0F, 10.0F, -4.0F, 0.27F, -0.14F, -0.44F, 4.0F, 1.0F, 0.0F, 0.26F, 0.1F, 10.0F, 0.3F, 0.45F);
            case DoubleBarrel -> new GunView(14.0F, 17.0F, 11.0F, -9.0F, 0.27F, -0.154F, -0.525F, 4.0F, 1.0F, 0.0F, 0.26F, 0.09F, 11.0F, 0.3F, 0.5F);
            case Spiker -> new GunView(15.0F, 10.0F, 8.5F, -5.0F, 0.26F, -0.098F, -0.66F, 4.0F, 1.5F, 0.0F, 0.4F, 0.03F, 2.0F, 0.0F, 0.6F);
            case Ultralaser -> new GunView(16.0F, 23.0F, 10.0F, -15.0F, 0.27F, -0.15F, -0.46F, 4.0F, 1.0F, 0.0F, 0.26F, 0.14F, 12.0F, 0.5F, 0.38F);
            case Scp127 -> new GunView(14.5F, 14.5F, 12.0F, -3.0F, 0.27F, -0.13F, -0.44F, 4.0F, 1.0F, 0.0F, 0.3F, 0.03F, 2.6F, 0.18F, 0.6F);
            case MicroHid -> new GunView(30.0F, 28.0F, 6.0F, -14.0F, 0.3F, 0.02F, -0.35F, 8.0F, 4.0F, -6.0F, 0.55F, 0.012F, 0.8F, 0.0F, 0.65F);
            case CombatShotgun -> new GunView(16.0F, 24.0F, 10.0F, -2.0F, 0.27F, -0.13F, -0.42F, 4.0F, 1.0F, 0.0F, 0.24F, 0.06F, 7.0F, 0.28F, 0.5F);
            case MachineGun -> new GunView(17.0F, 19.0F, 10.0F, -16.0F, 0.28F, -0.16F, -0.46F, 3.5F, 1.0F, 0.0F, 0.32F, 0.035F, 3.5F, 0.26F, 0.35F);
            case Flamethrower -> new GunView(18.0F, 14.0F, 10.5F, -14.0F, 0.27F, -0.03F, -0.44F, 4.0F, 1.5F, 0.0F, 0.36F, 0.006F, 0.4F, 0.0F, 0.55F);
            case ScarletRifle -> new GunView(15.5F, 23.0F, 10.75F, -6.0F, 0.27F, -0.13F, -0.42F, 4.0F, 1.0F, 0.0F, 0.24F, 0.03F, 2.6F, 0.22F, 0.5F);
        };
    }
}
