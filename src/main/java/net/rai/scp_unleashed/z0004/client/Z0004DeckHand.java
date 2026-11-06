package net.rai.scp_unleashed.z0004.client;

import net.rai.scp_unleashed.z0004.Z0004DeckSlot;
import net.rai.scp_unleashed.z0004.Z0004Ship;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class Z0004DeckHand implements GeoAnimatable {
    public static final RawAnimation FallHold = RawAnimation.begin().thenPlayAndHold("deck_fall");
    public static final RawAnimation DuckOnce = RawAnimation.begin().thenPlay("deck_duck");
    public static final int Blend = 6;
    private final AnimatableInstanceCache Cache = new SingletonAnimatableInstanceCache(this);
    private final Z0004Ship Ship;
    private final Z0004DeckSlot Slot;
    private final RawAnimation Loop;
    private AnimationController<Z0004DeckHand> Action;
    private int SeenVolley = Integer.MIN_VALUE;
    private float Partial;

    public Z0004DeckHand(Z0004Ship Ship, Z0004DeckSlot Slot) {
        this.Ship = Ship;
        this.Slot = Slot;
        this.Loop = RawAnimation.begin().thenLoop(Slot.Loop());
    }

    public Z0004DeckSlot slot() {
        return Slot;
    }

    public Z0004Ship ship() {
        return Ship;
    }

    public void update(float PartialTick) {
        Partial = PartialTick;
        int Volleys = Ship.volleys();
        if (SeenVolley == Integer.MIN_VALUE) {
            SeenVolley = Volleys;
        } else if (Volleys != SeenVolley) {
            SeenVolley = Volleys;
            if (Slot.gunCrew() && Action != null && !Ship.isDeadOrDying()) {
                Action.tryTriggerAnimation("duck");
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "deck", Blend, State -> State.setAndContinue(Ship.isDeadOrDying() ? FallHold : Loop)));
        Action = new AnimationController<>(this, "action", 3, State -> PlayState.STOP);
        if (!Slot.captain()) {
            Action.triggerableAnim("duck", DuckOnce);
        }
        Controllers.add(Action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    public double getTick(Object Owner) {
        return Ship.tickCount + Partial;
    }
}
