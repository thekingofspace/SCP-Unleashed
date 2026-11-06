package net.rai.scp_unleashed.scp093;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp093.client.Scp093ClientHooks;

public final class Scp093Gait {
    public static final double Response = 0.3;
    public static final double StartShare = 0.25;
    public static final double StopShare = 0.12;
    public static final int StartTicks = 2;
    public static final int StopTicks = 5;
    public static final double Wrap = 64.0;
    public static final double LateShare = 0.5;
    public static final double Hearing = 80.0;
    public static final float ThudVolume = 1.0f;
    public static final float ImpactVolume = 1.1f;
    public static final int GroundSearch = 3;
    public static final int PlantDust = 14;
    public static final int PlantChips = 8;
    public static final int PlantPuffs = 3;
    public static final double PlantSpread = 1.6;
    public static final double ImpactSpread = 3.2;
    public static final double TurnArm = 1.5;

    public record Profile(double CycleTicks, double Reference, double MinRate, double MaxRate, double[] Contacts, float[] Weights) {
    }

    private final Profile Crawl;
    private double Phase;
    private double PhaseO;
    private double Speed;
    private double Rate;
    private boolean Striding;
    private int Eager;
    private int Calm;
    private int Pending;
    private float Partial;

    public Scp093Gait(Profile Crawl) {
        this.Crawl = Crawl;
    }

    public boolean striding() {
        return Striding;
    }

    public double rate() {
        return Rate;
    }

    public void setPartial(float Value) {
        Partial = Mth.clamp(Value, 0.0f, 1.0f);
    }

    public double cycle() {
        double Value = PhaseO + (Phase - PhaseO) * Partial;
        return Math.min(Value - Math.floor(Value), 0.9999);
    }

    public void tick(Scp093Torso Owner) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        double Dy = Owner.isClimbing() ? Owner.getY() - Owner.yo : 0.0;
        double Turn = Math.abs(Mth.wrapDegrees(Owner.yBodyRot - Owner.yBodyRotO)) * Mth.DEG_TO_RAD * TurnArm;
        Speed += (Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz) + Turn - Speed) * Response;
        boolean Free = Owner.isAlive() && !Owner.isPassenger() && !Owner.locksMovement();
        if (Pending != 0) {
            for (int Index = 0; Index < Crawl.Contacts().length; Index++) {
                if ((Pending & 1 << Index) != 0 && Free && Striding) {
                    plant(Owner, Index);
                }
            }
            Pending = 0;
        }
        if (!Striding) {
            Eager = Free && Speed > Crawl.Reference() * StartShare ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Striding = true;
                Eager = 0;
                Calm = 0;
                Phase = 0.0;
                PhaseO = 0.0;
            }
            return;
        }
        Calm = !Free || Speed < Crawl.Reference() * StopShare ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Striding = false;
            Calm = 0;
            Pending = 0;
            Phase = 0.0;
            PhaseO = 0.0;
            Rate = 0.0;
            return;
        }
        Rate = Mth.clamp(Speed / Crawl.Reference(), Crawl.MinRate(), Crawl.MaxRate());
        Phase += Rate / Crawl.CycleTicks();
        for (int Index = 0; Index < Crawl.Contacts().length; Index++) {
            contact(Owner, Index, Free);
        }
        if (Phase >= Wrap) {
            Phase -= Wrap;
            PhaseO -= Wrap;
        }
    }

    private void contact(Scp093Torso Owner, int Index, boolean Free) {
        double Contact = Crawl.Contacts()[Index];
        double Before = Math.floor(PhaseO - Contact);
        double After = Math.floor(Phase - Contact);
        if (After <= Before) {
            return;
        }
        double Share = (Contact + After - PhaseO) / (Phase - PhaseO);
        if (Share >= LateShare) {
            Pending |= 1 << Index;
        } else if (Free) {
            plant(Owner, Index);
        }
    }

    private boolean audible(Scp093Torso Owner) {
        return !Owner.isSilent() && Owner.level().hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), Hearing);
    }

    private void plant(Scp093Torso Owner, int Index) {
        if (!audible(Owner)) {
            return;
        }
        float Weight = (float) Mth.clamp(0.75 + 0.25 * Rate, 0.6, 1.1) * Crawl.Weights()[Index];
        thud(Owner, Owner.plantSpot(Index), ThudVolume * Weight, Weight, PlantSpread, 1);
    }

    public void impact(Scp093Torso Owner, Vec3 Spot, float Weight) {
        if (!audible(Owner)) {
            return;
        }
        thud(Owner, Spot, ImpactVolume * Weight, Weight * 1.4f, ImpactSpread, 2);
    }

    private static void thud(Scp093Torso Owner, Vec3 Spot, float Volume, float Weight, double Spread, int Bursts) {
        Level World = Owner.level();
        RandomSource Random = Owner.getRandom();
        float Pitch = 0.9f + Random.nextFloat() * 0.15f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp093Registry.ThudLayers) {
            World.playLocalSound(Spot.x, Spot.y, Spot.z, Layer.get(), Owner.getSoundSource(), Volume, Pitch, false);
        }
        BlockPos Below = BlockPos.containing(Spot.x, Spot.y - 0.2, Spot.z);
        BlockState Ground = World.getBlockState(Below);
        for (int Step = 0; Step < GroundSearch && Ground.isAir(); Step++) {
            Below = Below.below();
            Ground = World.getBlockState(Below);
        }
        if (!Ground.isAir() && Ground.getRenderShape() != RenderShape.INVISIBLE) {
            dust(World, Random, Spot, Below.getY() + 1.0, Ground, Spread, Bursts);
        }
        Scp093ClientHooks.thud(Spot.x, Spot.y, Spot.z, Weight);
    }

    private static void dust(Level World, RandomSource Random, Vec3 Spot, double Top, BlockState Ground, double Spread, int Bursts) {
        BlockParticleOption Pillar = new BlockParticleOption(ParticleTypes.DUST_PILLAR, Ground);
        BlockParticleOption Chips = new BlockParticleOption(ParticleTypes.BLOCK, Ground);
        for (int Index = 0; Index < PlantDust * Bursts; Index++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Reach = Spread * (0.35 + 0.65 * Random.nextDouble());
            World.addParticle(Pillar, Spot.x + Math.cos(Angle) * Reach, Top, Spot.z + Math.sin(Angle) * Reach, 0.0, 0.08 + Random.nextDouble() * 0.12, 0.0);
        }
        for (int Index = 0; Index < PlantChips * Bursts; Index++) {
            World.addParticle(Chips, Spot.x + Random.nextGaussian() * Spread * 0.4, Top + 0.1, Spot.z + Random.nextGaussian() * Spread * 0.4,
                    Random.nextGaussian() * 0.08, 0.15 + Random.nextDouble() * 0.15, Random.nextGaussian() * 0.08);
        }
        for (int Index = 0; Index < PlantPuffs * Bursts; Index++) {
            World.addParticle(ParticleTypes.DUST_PLUME, Spot.x + Random.nextGaussian() * Spread * 0.3, Top + 0.2, Spot.z + Random.nextGaussian() * Spread * 0.3,
                    Random.nextGaussian() * 0.03, 0.03 + Random.nextDouble() * 0.04, Random.nextGaussian() * 0.03);
        }
    }
}
