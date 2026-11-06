package net.rai.scp_unleashed.scp179.client;

import net.rai.scp_unleashed.scp179.Scp179Omen;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp179Figure implements GeoAnimatable {
    public static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation Point = RawAnimation.begin().thenPlayAndHold("point");
    public static final RawAnimation Drift = RawAnimation.begin().thenLoop("drift");
    public static final int TurnTicks = 10;

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private Scp179Omen Omen = Scp179Omen.None;
    private double Tick;
    private float Glow = 1.0f;

    public void update(Scp179Omen Seen, double Now, float Shimmer) {
        Omen = Seen;
        Tick = Now;
        Glow = Shimmer;
    }

    public float glow() {
        return Glow;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", TurnTicks, State -> State.setAndContinue(Omen.isActive() ? Point : Idle)));
        Controllers.add(new AnimationController<>(this, "hair", 0, State -> State.setAndContinue(Drift)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    public double getTick(Object Animatable) {
        return Tick;
    }
}
