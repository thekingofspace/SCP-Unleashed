package net.rai.scp_unleashed.scp096;

import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

public final class Scp096Goals {
    private Scp096Goals() {
    }

    public static class LookAround extends RandomLookAroundGoal {
        private final Scp096ShyGuy Guy;

        public LookAround(Scp096ShyGuy Owner) {
            super(Owner);
            Guy = Owner;
        }

        @Override
        public boolean canUse() {
            return Guy.isPausing() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Guy.isPausing() && super.canContinueToUse();
        }
    }
}
