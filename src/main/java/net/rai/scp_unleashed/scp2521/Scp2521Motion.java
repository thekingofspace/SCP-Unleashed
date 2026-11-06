package net.rai.scp_unleashed.scp2521;

import net.minecraft.util.Mth;

public final class Scp2521Motion {
    public static final int StateIdle = 0;
    public static final int StateRise = 1;
    public static final int StateGrab = 2;
    public static final int StateCarry = 3;
    public static final int StateStare = 4;
    public static final int StateSink = 5;
    public static final int StateContained = 6;
    public static final int StateAbsorb = 7;
    public static final int StateBreak = 8;
    public static final float RiseSeconds = 2.5f;
    public static final float AbsorbSeconds = 2.0f;
    public static final float SinkSeconds = 2.0f;

    public record Phase(int State, float Seconds) {
    }

    public record Sway(float Amount, float Speed, float Coil, float Flare, float Trail) {
    }

    private Scp2521Motion() {
    }

    public static float ease(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static Phase phase(int Mode, float Seconds) {
        return switch (Mode) {
            case Scp2521.ModeRise -> Seconds < RiseSeconds ? new Phase(StateRise, Seconds) : new Phase(StateIdle, Seconds - RiseSeconds);
            case Scp2521.ModeGrab -> new Phase(StateGrab, Seconds);
            case Scp2521.ModeCarry -> new Phase(StateCarry, Seconds);
            case Scp2521.ModeStare -> new Phase(StateStare, Seconds);
            case Scp2521.ModeSink -> new Phase(StateSink, Seconds);
            case Scp2521.ModeContained -> new Phase(StateContained, Seconds);
            case Scp2521.ModeHolding -> Seconds < RiseSeconds ? new Phase(StateRise, Seconds)
                    : Seconds < RiseSeconds + AbsorbSeconds ? new Phase(StateAbsorb, Seconds - RiseSeconds) : new Phase(StateContained, Seconds - RiseSeconds - AbsorbSeconds);
            case Scp2521.ModeBreak -> new Phase(StateBreak, Seconds);
            case Scp2521.ModeReform -> Seconds < SinkSeconds ? new Phase(StateSink, Seconds)
                    : Seconds < SinkSeconds + RiseSeconds ? new Phase(StateRise, Seconds - SinkSeconds) : new Phase(StateContained, Seconds - SinkSeconds - RiseSeconds);
            default -> new Phase(StateIdle, Seconds);
        };
    }

    public static Sway sway(Phase Current) {
        float Seconds = Current.Seconds();
        return switch (Current.State()) {
            case StateRise -> new Sway(0.6f, 1.0f, 0.0f, 0.0f, 1.0f - ease(Seconds / 2.2f));
            case StateGrab -> new Sway(1.3f, 2.0f, 0.0f, Mth.sin((float) Math.PI * Mth.clamp((Seconds - 0.8f) / 0.6f, 0.0f, 1.0f)) * 0.8f, 0.0f);
            case StateCarry -> new Sway(0.8f, 1.0f, 0.0f, 0.0f, -ease((Seconds - 0.3f) / 1.2f));
            case StateStare -> new Sway(0.55f, 0.7f, 0.0f, 0.0f, 0.0f);
            case StateSink -> new Sway(0.7f, 1.0f, 0.0f, 0.0f, -ease(Seconds / 2.0f));
            case StateContained -> new Sway(0.3f, 0.5f, 0.6f + 0.4f * Mth.sin(Seconds / 8.0f * Mth.TWO_PI), 0.0f, 0.0f);
            case StateAbsorb -> new Sway(0.5f, 1.0f, ease(Seconds / 2.0f) * 0.8f, 0.0f, 0.0f);
            case StateBreak -> new Sway(1.5f, 3.0f, 0.0f, Mth.sin((float) Math.PI * Math.min(1.0f, Seconds / 0.6f)) * 1.4f, 0.0f);
            default -> new Sway(1.0f, 1.0f, 0.0f, 0.0f, 0.0f);
        };
    }

    public static float speed(Scp2521 Creature) {
        return sway(phase(Creature.mode(), Creature.modeSeconds(0.0f))).Speed();
    }
}
