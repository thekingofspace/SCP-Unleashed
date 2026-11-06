package net.rai.scp_unleashed.scp066;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class Scp066Goals {
    public static final int PreyInterval = 10;

    private Scp066Goals() {
    }

    public static final class Stroll extends RandomStrollGoal {
        private final Scp066Entity Heap;
        private final byte Mood;

        public Stroll(Scp066Entity Heap, byte Mood, double Pace, int Interval) {
            super(Heap, Pace, Interval);
            this.Heap = Heap;
            this.Mood = Mood;
        }

        @Override
        public boolean canUse() {
            return Heap.mood() == Mood && Heap.getTarget() == null && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Heap.mood() == Mood && Heap.getTarget() == null && super.canContinueToUse();
        }
    }

    public static final class Hunt extends MeleeAttackGoal {
        private final Scp066Entity Heap;

        public Hunt(Scp066Entity Heap) {
            super(Heap, Scp066Entity.HuntPace, true);
            this.Heap = Heap;
        }

        @Override
        public boolean canUse() {
            return Heap.isHostile() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Heap.isHostile() && super.canContinueToUse();
        }
    }

    public static final class Prey<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
        private final Scp066Entity Heap;

        public Prey(Scp066Entity Heap, Class<T> Kind, @Nullable Predicate<LivingEntity> Filter) {
            super(Heap, Kind, PreyInterval, true, false, Filter);
            this.Heap = Heap;
        }

        @Override
        public boolean canUse() {
            return Heap.isHostile() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Heap.isHostile() && super.canContinueToUse();
        }
    }
}
