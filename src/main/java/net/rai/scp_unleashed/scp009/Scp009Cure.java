package net.rai.scp_unleashed.scp009;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp009Cure {
    private Scp009Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (!Scp009Doom.isDoomed(Target) && !isSlowed(Target)) {
            return;
        }
        Scp009Doom.clear(Target);
        Event.markCured();
    }

    public static boolean isSlowed(LivingEntity Target) {
        return hasSlow(Target.getAttribute(Attributes.MOVEMENT_SPEED)) || hasSlow(Target.getAttribute(Attributes.FLYING_SPEED));
    }

    private static boolean hasSlow(@Nullable AttributeInstance Speed) {
        return Speed != null && Speed.hasModifier(Scp009Doom.SlowId);
    }
}
