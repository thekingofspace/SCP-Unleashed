package net.rai.scp_unleashed.scp082;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public final class Scp082Goals {
    public static final int WanderInterval = 90;

    private Scp082Goals() {
    }

    public static class Wander extends WaterAvoidingRandomStrollGoal {
        private final Scp082Fernand Fernand;

        public Wander(Scp082Fernand Owner, double Speed) {
            super(Owner, Speed);
            Fernand = Owner;
            setInterval(WanderInterval);
        }

        @Override
        public boolean canUse() {
            return Fernand.isFree() && !Fernand.isTalking() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Fernand.isFree() && super.canContinueToUse();
        }
    }
}
