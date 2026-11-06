package net.rai.scp_unleashed.scp529;

import net.rai.scp_unleashed.motion.ActionClock;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Map;
import java.util.Set;

public class Scp529GaitController extends AnimationController<Scp529Cat> {
    public static final Set<String> StrideClips = Set.of("walk", "sneak", "run");
    public static final Set<String> RestClips = Set.of("lie_down", "get_up");
    public static final int LoopBlend = 3;
    public static final int ActionBlend = 2;
    public static final int RestBlend = 8;
    private static final Map<String, RawAnimation> Plays = Map.ofEntries(
            Map.entry("sit_down", RawAnimation.begin().thenPlayAndHold("sit_down")),
            Map.entry("stand_up", RawAnimation.begin().thenPlayAndHold("stand_up")),
            Map.entry("lie_down", RawAnimation.begin().thenPlayAndHold("lie_down")),
            Map.entry("get_up", RawAnimation.begin().thenPlayAndHold("get_up")),
            Map.entry("stretch", RawAnimation.begin().thenPlayAndHold("stretch")),
            Map.entry("groom", RawAnimation.begin().thenPlayAndHold("groom")),
            Map.entry("knead", RawAnimation.begin().thenPlayAndHold("knead")),
            Map.entry("swat", RawAnimation.begin().thenPlayAndHold("swat")),
            Map.entry("hiss", RawAnimation.begin().thenPlayAndHold("hiss")),
            Map.entry("yowl", RawAnimation.begin().thenPlayAndHold("yowl")),
            Map.entry("bury", RawAnimation.begin().thenPlayAndHold("bury")));

    public Scp529GaitController(Scp529Cat Josie, String Name, int TransitionTicks, AnimationStateHandler<Scp529Cat> Handler) {
        super(Josie, Name, TransitionTicks, Handler);
    }

    public static RawAnimation play(String Clip) {
        return Plays.getOrDefault(Clip, Plays.get("stretch"));
    }

    public static int blendFor(@Nullable String Shown, boolean Lying) {
        if (Shown != null) {
            return RestClips.contains(Shown) ? RestBlend : ActionBlend;
        }
        return Lying ? RestBlend : LoopBlend;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.actionClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name())) {
                animatable.actionClock().learn(Clip, currentAnimation.animation().length());
                return ActionClock.catchUp(animatable.actionElapsed(), transitionLength);
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        if (!StrideClips.contains(currentAnimation.animation().name())) {
            return Adjusted;
        }
        return animatable.stride().cycle(animatable.renderPartial()) * currentAnimation.animation().length();
    }
}
