package net.rai.scp_unleashed.anomaly;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0003.Z0003Petrify;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Targeting {
    public static final String SparedPrefix = "warhead";

    private Targeting() {
    }

    public static boolean isStatue(@Nullable Entity Subject) {
        return Z0003Petrify.isStatue(Subject);
    }

    public static boolean isTargetable(@Nullable Entity Subject) {
        return Subject != null && !isStatue(Subject);
    }

    public static boolean canWitness(@Nullable Entity Subject) {
        return Subject != null && !isStatue(Subject);
    }

    public static boolean isAnomalous(DamageSource Source) {
        Optional<ResourceLocation> Kind = Source.typeHolder().unwrapKey().map(Key -> Key.location());
        return Kind.isPresent() && Kind.get().getNamespace().equals(Scp_unleashed.ModId) && !Kind.get().getPath().startsWith(SparedPrefix);
    }

    public static boolean drop(Mob Hunter) {
        boolean Dropped = false;
        if (isStatue(Hunter.getTarget())) {
            Hunter.setTarget(null);
            Dropped = true;
        }
        Brain<?> Mind = Hunter.getBrain();
        if (Mind.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            Optional<LivingEntity> Remembered = Mind.getMemory(MemoryModuleType.ATTACK_TARGET);
            if (Remembered.isPresent() && isStatue(Remembered.get())) {
                Mind.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                Dropped = true;
            }
        }
        return Dropped;
    }

    @SubscribeEvent
    public static void mobTick(EntityTickEvent.Pre Event) {
        if (Event.getEntity() instanceof Mob Hunter && !Hunter.level().isClientSide) {
            drop(Hunter);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void statueHurt(LivingIncomingDamageEvent Event) {
        if (isStatue(Event.getEntity()) && isAnomalous(Event.getSource())) {
            Event.setCanceled(true);
        }
    }
}
