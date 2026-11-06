package net.rai.scp_unleashed.scp323;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class Scp323Gait {
    public static final double Response = 0.35;
    public static final double StartShare = 0.3;
    public static final double StopShare = 0.15;
    public static final int StartTicks = 2;
    public static final int StopTicks = 3;
    public static final double Wrap = 64.0;
    public static final double LateShare = 0.5;
    public static final double Hearing = 40.0;
    public static final float StepVolume = 0.85f;
    public static final float HeavyVolume = 0.6f;
    public static final double StalkCycleTicks = 26.0;
    public static final double StalkReference = 0.085;
    public static final double SprintCycleTicks = 15.0;
    public static final double SprintReference = 0.36;
    public static final double CrawlCycleTicks = 24.0;
    public static final double CrawlReference = 0.08;
    public static final double RunAbove = 0.2;
    public static final double WalkBelow = 0.15;
    public static final Profile Stalk = new Profile(StalkCycleTicks, StalkReference, 0.45, 2.2, new double[]{0.0, 0.5}, false);
    public static final Profile Sprint = new Profile(SprintCycleTicks, SprintReference, 0.55, 1.5, new double[]{0.0, 0.5}, true);
    public static final Profile Crawl = new Profile(CrawlCycleTicks, CrawlReference, 0.45, 2.4, new double[]{0.0, 0.5}, false);

    public record Profile(double CycleTicks, double Reference, double MinRate, double MaxRate, double[] Contacts, boolean Heavy) {
    }

    private double Phase;
    private double PhaseO;
    private double Speed;
    private double Rate;
    private boolean Striding;
    private boolean Running;
    private boolean Crawling;
    private int Eager;
    private int Calm;
    private int Pending;
    private float Partial;

    public boolean striding() {
        return Striding;
    }

    public boolean running() {
        return Running;
    }

    public boolean crawling() {
        return Crawling;
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

    private Profile current() {
        return Crawling ? Crawl : Running ? Sprint : Stalk;
    }

    public void tick(Scp323Beast Owner) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        Speed += (Math.sqrt(Dx * Dx + Dz * Dz) - Speed) * Response;
        boolean Free = Owner.isAlive() && Owner.onGround() && !Owner.isInWater() && !Owner.isPassenger() && !Owner.busy();
        boolean Low = Owner.isCrawling();
        Profile Active = current();
        if (Pending != 0) {
            for (int Index = 0; Index < Active.Contacts().length; Index++) {
                if ((Pending & 1 << Index) != 0 && Free && Striding) {
                    footfall(Owner, Active, Index);
                }
            }
            Pending = 0;
        }
        if (!Striding) {
            Crawling = Low;
            Eager = Free && Speed > Stalk.Reference() * StartShare ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Striding = true;
                Running = !Low && Speed > RunAbove;
                Eager = 0;
                Calm = 0;
                Phase = 0.0;
                PhaseO = 0.0;
            }
            return;
        }
        Calm = !Free || Speed < Stalk.Reference() * StopShare ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Striding = false;
            Running = false;
            Calm = 0;
            Pending = 0;
            Phase = 0.0;
            PhaseO = 0.0;
            Rate = 0.0;
            return;
        }
        Crawling = Low;
        if (Low) {
            Running = false;
        } else if (Running && Speed < WalkBelow) {
            Running = false;
        } else if (!Running && Speed > RunAbove) {
            Running = true;
        }
        Active = current();
        Rate = Mth.clamp(Speed / Active.Reference(), Active.MinRate(), Active.MaxRate());
        Phase += Rate / Active.CycleTicks();
        for (int Index = 0; Index < Active.Contacts().length; Index++) {
            contact(Owner, Active, Index, Free);
        }
        if (Phase >= Wrap) {
            Phase -= Wrap;
            PhaseO -= Wrap;
        }
    }

    private void contact(Scp323Beast Owner, Profile Active, int Index, boolean Free) {
        double Contact = Active.Contacts()[Index];
        double Before = Math.floor(PhaseO - Contact);
        double After = Math.floor(Phase - Contact);
        if (After <= Before) {
            return;
        }
        double Share = (Contact + After - PhaseO) / (Phase - PhaseO);
        if (Share >= LateShare) {
            Pending |= 1 << Index;
        } else if (Free) {
            footfall(Owner, Active, Index);
        }
    }

    private void footfall(Scp323Beast Owner, Profile Active, int Index) {
        Level World = Owner.level();
        if (Owner.isSilent() || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), Hearing)) {
            return;
        }
        RandomSource Random = Owner.getRandom();
        float Weight = Active.Heavy() ? 1.15f : Crawling ? 0.7f : 1.0f;
        float Pitch = (Index % 2 == 0 ? 1.0f : 0.95f) * (0.92f + Random.nextFloat() * 0.12f);
        World.playLocalSound(Owner.getX(), Owner.getY(), Owner.getZ(), Scp323Registry.Step.get(), Owner.getSoundSource(), StepVolume * Weight, Pitch, false);
        if (Active.Heavy() || Random.nextInt(3) == 0) {
            World.playLocalSound(Owner.getX(), Owner.getY(), Owner.getZ(), Scp323Registry.StepHeavy.get(), Owner.getSoundSource(), HeavyVolume * Weight, Pitch * 0.97f, false);
        }
    }
}
