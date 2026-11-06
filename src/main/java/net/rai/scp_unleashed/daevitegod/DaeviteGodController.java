package net.rai.scp_unleashed.daevitegod;

import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DaeviteGodController extends AnimationController<DaeviteGod> {
    private static final Map<String, RawAnimation> Plays = new ConcurrentHashMap<>();

    public DaeviteGodController(DaeviteGod God, String Name, int Blend, AnimationStateHandler<DaeviteGod> Handler) {
        super(God, Name, Blend, Handler);
    }

    public static RawAnimation play(String Clip) {
        return Plays.computeIfAbsent(Clip, Name -> RawAnimation.begin().thenPlayAndHold(Name));
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Name = currentAnimation.animation().name();
            String Action = animatable.actionClip();
            if (Action != null && (Name.equals(Action) || Name.equals("god_float_blink") && Action.equals("god_blink"))) {
                return animatable.clipTicks();
            }
            String Phase = animatable.phaseClip();
            if (Phase != null && (Name.equals(Phase) || Name.equals("god_float_entrance") || Name.equals("god_float_transform") || Name.equals("god_float_death"))) {
                return animatable.phaseClipTicks();
            }
            DaeviteGodGait Gait = animatable.gait();
            if (Gait != null && (Name.equals("king_walk") || Name.equals("king_run"))) {
                return Gait.cycle() * currentAnimation.animation().length();
            }
        }
        return super.adjustTick(Tick);
    }
}
