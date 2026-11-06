package net.rai.scp_unleashed.grenade;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public final class GrenadeDaze {
    private static final Map<Entity, Long> Until = new WeakHashMap<>();

    private GrenadeDaze() {
    }

    public static void daze(Mob Target, int Ticks, @Nullable Entity Source) {
        long End = Target.level().getGameTime() + Ticks;
        Until.merge(Target, End, Math::max);
        Target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Ticks, GrenadeTuning.StunAmplifier), Source);
        Target.setTarget(null);
        Target.setAggressive(false);
        Target.getNavigation().stop();
        Target.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        Target.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
    }

    public static boolean isDazed(LivingEntity Target) {
        Long End = Until.get(Target);
        if (End == null) {
            return false;
        }
        if (Target.level().getGameTime() >= End) {
            Until.remove(Target);
            return false;
        }
        return true;
    }

    public static boolean wake(LivingEntity Target) {
        Long End = Until.remove(Target);
        return End != null && Target.level().getGameTime() < End;
    }

    public static void clear() {
        Until.clear();
    }
}
