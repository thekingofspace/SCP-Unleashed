package net.rai.scp_unleashed.daevitegod;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.registries.DeferredHolder;

public class DaeviteGodGait {
    public static final double WalkStride = 36.0 / 16.0;
    public static final double RunStride = 52.0 / 16.0;
    public static final double StrideStart = 0.035;
    public static final double StrideStop = 0.015;
    public static final double RunSpeed = 0.17;
    public static final double[] Contacts = {0.0, 0.5};
    public static final float StepVolume = 0.85f;
    public static final double StepHearing = 32.0;

    private double Phase;
    private double PhaseO;
    private boolean Striding;
    private boolean Running;
    private float Partial;

    public void setPartial(float Value) {
        Partial = Value;
    }

    public boolean striding() {
        return Striding;
    }

    public boolean running() {
        return Running;
    }

    public double cycle() {
        double Value = PhaseO + (Phase - PhaseO) * Partial;
        return Value - Math.floor(Value);
    }

    public void tick(DaeviteGod God) {
        PhaseO = Phase;
        if (!God.king() || !God.isAlive() || God.busy() || God.action() != DaeviteGod.ActionNone) {
            Striding = false;
            return;
        }
        double Speed = Math.sqrt(Mth.square(God.getX() - God.xo) + Mth.square(God.getZ() - God.zo));
        if (Striding ? Speed < StrideStop : Speed > StrideStart) {
            Striding = !Striding;
        }
        if (!Striding) {
            return;
        }
        Running = Speed > RunSpeed;
        double Before = Phase;
        Phase += Speed / (Running ? RunStride : WalkStride);
        for (double Contact : Contacts) {
            double Mark = Math.floor(Before - Contact) + 1.0 + Contact;
            if (Mark <= Phase) {
                footfall(God);
            }
        }
        if (Phase > 1000.0) {
            Phase -= 1000.0;
            PhaseO -= 1000.0;
        }
    }

    private void footfall(DaeviteGod God) {
        if (God.isSilent() || !God.level().hasNearbyAlivePlayer(God.getX(), God.getY(), God.getZ(), StepHearing)) {
            return;
        }
        float Pitch = 0.85f + God.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : DaeviteGodRegistry.StepLayers) {
            God.level().playLocalSound(God.getX(), God.getY(), God.getZ(), Layer.get(), SoundSource.HOSTILE, StepVolume * (Running ? 1.15f : 1.0f), Pitch, false);
        }
    }
}
