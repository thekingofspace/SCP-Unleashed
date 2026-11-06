package net.rai.scp_unleashed.scp354;

import net.rai.scp_unleashed.motion.ActionClock;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class Scp354StageController<T extends GeoAnimatable> extends AnimationController<T> {
    public static final Set<String> GaitClips = Set.of("walk", "run", "stalk");
    private final Function<T, String> Clip;
    private final ToDoubleFunction<T> Elapsed;
    @Nullable
    private final Function<T, Scp354Gait> Stride;

    public Scp354StageController(T Owner, String Name, Function<T, String> Clip, ToDoubleFunction<T> Elapsed, AnimationStateHandler<T> Handler) {
        this(Owner, Name, Clip, Elapsed, null, Handler);
    }

    public Scp354StageController(T Owner, String Name, Function<T, String> Clip, ToDoubleFunction<T> Elapsed, @Nullable Function<T, Scp354Gait> Stride,
                                 AnimationStateHandler<T> Handler) {
        super(Owner, Name, 0, Handler);
        this.Clip = Clip;
        this.Elapsed = Elapsed;
        this.Stride = Stride;
    }

    public boolean playing(String Name) {
        return currentAnimation != null && Name.equals(currentAnimation.animation().name()) && animationState != State.STOPPED;
    }

    public static double catchUp(double Spent, double Blend) {
        if (Blend <= 0.0 || Spent >= Blend * 3.0) {
            return Spent;
        }
        return Spent <= Blend ? 0.0 : (Spent - Blend) * 1.5;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Name = Clip.apply(animatable);
            if (Name != null && Name.equals(currentAnimation.animation().name())) {
                if (animatable instanceof ActionClock.Owner Owner) {
                    Owner.actionClock().learn(Name, currentAnimation.animation().length());
                }
                return catchUp(Elapsed.applyAsDouble(animatable), transitionLength);
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (Stride == null || animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        String Name = currentAnimation.animation().name();
        Scp354Gait Gait = Stride.apply(animatable);
        if (Gait == null || !GaitClips.contains(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
