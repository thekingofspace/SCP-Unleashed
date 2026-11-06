package net.rai.scp_unleashed.scp3199;

import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;
import java.util.Set;

public class Scp3199GaitController extends AnimationController<Scp3199> {
    public static final Set<String> StrideClips = Set.of("walk", "run");
    public static final int Blend = 6;
    public static final int StrideBlend = 2;
    public static final int VariantBlend = 4;
    public static final int SwimEnter = 4;
    public static final int SwimLeave = 6;
    public static final int DeathSpan = 46;
    public static final int VariantTicks = 80;
    public static final int VariantGapMin = 60;
    public static final int VariantGapSpread = 100;
    public static final int FirstGapMin = 40;
    public static final List<String> VariantNames = List.of("idle_twitch", "idle_crane", "idle_sniff", "idle_ruffle", "idle_flex", "idle_shudder");
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    public static final RawAnimation SwimLoop = RawAnimation.begin().thenLoop("swim");
    public static final RawAnimation RegurgitateHold = RawAnimation.begin().thenPlayAndHold("regurgitate");
    public static final RawAnimation DeathHold = RawAnimation.begin().thenPlayAndHold("death");
    public static final List<RawAnimation> VariantHolds = VariantNames.stream().map(Name -> RawAnimation.begin().thenPlayAndHold(Name)).toList();

    private int SeenTick = Integer.MIN_VALUE;
    private int WetFor;
    private int DryFor;
    private boolean Swimming;
    private String Aligned = "";
    private double Offset;
    private int Chosen = Choice.Idle;
    private int Variant = -1;
    private int LastVariant = -1;
    private int VariantEnd;
    private int NextVariant = Integer.MAX_VALUE;

    public Scp3199GaitController(Scp3199 Owner) {
        super(Owner, "body", Blend, Scp3199GaitController::choose);
    }

    private static PlayState choose(AnimationState<Scp3199> Event) {
        return ((Scp3199GaitController) Event.getController()).pick(Event);
    }

    private void observe() {
        if (animatable.tickCount == SeenTick) {
            return;
        }
        SeenTick = animatable.tickCount;
        if (animatable.isSwimmingNow()) {
            WetFor++;
            DryFor = 0;
        } else {
            DryFor++;
            WetFor = 0;
        }
        if (!Swimming && WetFor >= SwimEnter) {
            Swimming = true;
        } else if (Swimming && DryFor >= SwimLeave) {
            Swimming = false;
        }
    }

    public boolean swimming() {
        return Swimming;
    }

    public double strideOffset() {
        return Aligned.isEmpty() ? 0.0 : Offset;
    }

    public int variant() {
        return Variant;
    }

    private int gap(int Minimum) {
        return Minimum + animatable.getRandom().nextInt(VariantGapSpread);
    }

    private PlayState play(AnimationState<Scp3199> Event, int Next, RawAnimation Wanted) {
        if (Next != Chosen) {
            transitionLength(Choice.blend(Chosen, Next));
            Chosen = Next;
        }
        return Event.setAndContinue(Wanted);
    }

    public PlayState pick(AnimationState<Scp3199> Event) {
        observe();
        if (animatable.isDeadOrDying()) {
            return play(Event, Choice.Death, DeathHold);
        }
        if (animatable.isLaying()) {
            return play(Event, Choice.Regurgitate, RegurgitateHold);
        }
        if (Swimming) {
            return play(Event, Choice.Swim, SwimLoop);
        }
        if (animatable.isStriding()) {
            return animatable.isRunning() ? play(Event, Choice.Run, RunLoop) : play(Event, Choice.Walk, WalkLoop);
        }
        return idle(Event);
    }

    private PlayState idle(AnimationState<Scp3199> Event) {
        int Now = animatable.tickCount;
        if (!Choice.resting(Chosen)) {
            Variant = -1;
            NextVariant = Now + gap(FirstGapMin);
        }
        if (Variant >= 0 && Now >= VariantEnd) {
            LastVariant = Variant;
            Variant = -1;
            NextVariant = Now + gap(VariantGapMin);
        }
        if (Variant < 0 && Now >= NextVariant) {
            int Pick = animatable.getRandom().nextInt(VariantNames.size() - (LastVariant >= 0 ? 1 : 0));
            if (LastVariant >= 0 && Pick >= LastVariant) {
                Pick++;
            }
            Variant = Pick;
            VariantEnd = Now + VariantBlend + VariantTicks;
        }
        if (Variant >= 0) {
            return play(Event, Choice.Variant, VariantHolds.get(Variant));
        }
        return play(Event, Choice.Idle, IdleLoop);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null || !StrideClips.contains(currentAnimation.animation().name())) {
            Aligned = "";
            return Adjusted;
        }
        double Cycle = animatable.stride().cycle(animatable.renderPartial());
        if (Double.isNaN(Cycle) || Double.isInfinite(Cycle)) {
            return Adjusted;
        }
        String Playing = currentAnimation.animation().name();
        if (!Playing.equals(Aligned)) {
            Aligned = Playing;
            Offset = Cycle;
        }
        double Shifted = Cycle - Offset;
        return (Shifted - Math.floor(Shifted)) * currentAnimation.animation().length();
    }

    public static final class Choice {
        public static final int Idle = 0;
        public static final int Variant = 1;
        public static final int Walk = 2;
        public static final int Run = 3;
        public static final int Swim = 4;
        public static final int Regurgitate = 5;
        public static final int Death = 6;

        private Choice() {
        }

        public static boolean stride(int Kind) {
            return Kind == Walk || Kind == Run;
        }

        public static boolean resting(int Kind) {
            return Kind == Idle || Kind == Variant;
        }

        public static int blend(int From, int To) {
            if (stride(To) && (resting(From) || stride(From))) {
                return StrideBlend;
            }
            if (resting(From) && resting(To)) {
                return VariantBlend;
            }
            return Blend;
        }
    }
}
