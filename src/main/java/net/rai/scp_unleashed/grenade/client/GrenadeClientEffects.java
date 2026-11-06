package net.rai.scp_unleashed.grenade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.grenade.GrenadeEntity;
import net.rai.scp_unleashed.grenade.GrenadeRegistry;
import net.rai.scp_unleashed.grenade.GrenadeTuning;

public final class GrenadeClientEffects {
    public static final int FizzSlow = 8;
    public static final int FizzFast = 4;
    public static final int FizzHurry = 20;
    public static final float SmokeChance = 0.35F;
    public static final double FlameSpread = 2.6;
    public static final int FlamesPerTick = 5;
    public static final float JetSpeed = 0.07F;

    private GrenadeClientEffects() {
    }

    public static void tick(GrenadeEntity Grenade) {
        Level World = Grenade.level();
        if (World != Minecraft.getInstance().level) {
            return;
        }
        RandomSource Random = World.getRandom();
        Vec3 Centre = Grenade.centre();
        switch (Grenade.getPhase()) {
            case GrenadeEntity.PhaseArmed -> fuse(World, Grenade, Centre, Random);
            case GrenadeEntity.PhaseBurning -> burn(World, Grenade, Random);
            case GrenadeEntity.PhaseVenting -> vent(World, Grenade, Centre, Random);
            default -> {
            }
        }
    }

    private static void fuse(Level World, GrenadeEntity Grenade, Vec3 Centre, RandomSource Random) {
        int Fuse = Grenade.getFuse();
        int Gap = Fuse <= FizzHurry ? FizzFast : FizzSlow;
        if (Grenade.tickCount % Gap == 0) {
            float Hurry = 1.0F - Mth.clamp(Fuse / (float) GrenadeTuning.FuseTicks, 0.0F, 1.0F);
            World.playLocalSound(Centre.x, Centre.y, Centre.z, GrenadeRegistry.Fuse.get(), SoundSource.BLOCKS, 0.3F + 0.2F * Hurry, 0.95F + 0.35F * Hurry, false);
        }
        if (Random.nextFloat() < SmokeChance) {
            World.addParticle(ParticleTypes.SMOKE, Centre.x, Centre.y + 0.12, Centre.z, 0.0, 0.02, 0.0);
        }
    }

    private static void burn(Level World, GrenadeEntity Grenade, RandomSource Random) {
        float Left = Mth.clamp(Grenade.getPhaseTicks() / (float) GrenadeTuning.BurnTicks, 0.0F, 1.0F);
        Vec3 Base = Grenade.position();
        int Count = Math.max(1, Math.round(FlamesPerTick * (0.35F + 0.65F * Left)));
        double Spread = FlameSpread * (0.6 + 0.4 * Left);
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Reach = Math.sqrt(Random.nextDouble()) * Spread;
            double X = Base.x + Math.cos(Angle) * Reach;
            double Z = Base.z + Math.sin(Angle) * Reach;
            World.addParticle(ParticleTypes.FLAME, X, Base.y + 0.05 + Random.nextDouble() * 0.25, Z, 0.0, 0.02 + Random.nextDouble() * 0.03, 0.0);
        }
        World.addParticle(ParticleTypes.FLAME, Base.x, Base.y + 0.15, Base.z, (Random.nextDouble() - 0.5) * 0.02, 0.06, (Random.nextDouble() - 0.5) * 0.02);
        if (Random.nextFloat() < 0.25F * Left) {
            World.addParticle(ParticleTypes.LAVA, Base.x, Base.y + 0.1, Base.z, 0.0, 0.0, 0.0);
        }
        if (Random.nextFloat() < 0.35F) {
            World.addParticle(ParticleTypes.LARGE_SMOKE, Base.x + (Random.nextDouble() - 0.5) * Spread, Base.y + 0.4, Base.z + (Random.nextDouble() - 0.5) * Spread,
                    0.0, 0.03, 0.0);
        }
    }

    private static void vent(Level World, GrenadeEntity Grenade, Vec3 Centre, RandomSource Random) {
        boolean Hissing = Grenade.clientData() instanceof GrenadeHissSound Playing && !Playing.isStopped();
        if (!Hissing && Grenade.getPhaseTicks() > 0) {
            GrenadeHissSound Hiss = new GrenadeHissSound(Grenade);
            Grenade.setClientData(Hiss);
            Minecraft.getInstance().getSoundManager().play(Hiss);
        }
        float Left = Mth.clamp(Grenade.getPhaseTicks() / (float) GrenadeTuning.ReleaseTicks, 0.0F, 1.0F);
        int Count = 1 + Math.round(3.0F * Left);
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Speed = JetSpeed * (0.6 + Random.nextDouble() * 0.8) * (0.5 + 0.5 * Left);
            World.addParticle(FissionRegistry.ToxicGasParticle.get(), Centre.x, Centre.y + 0.1, Centre.z,
                    Math.cos(Angle) * Speed, 0.02 + Random.nextDouble() * 0.04, Math.sin(Angle) * Speed);
        }
    }
}
