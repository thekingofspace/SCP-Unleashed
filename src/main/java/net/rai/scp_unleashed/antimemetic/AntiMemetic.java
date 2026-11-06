package net.rai.scp_unleashed.antimemetic;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class AntiMemetic {
    private AntiMemetic() {
    }

    public static boolean isProtected(@Nullable LivingEntity Target) {
        return Target != null && Target.hasEffect(AntiMemeticRegistry.Shielded);
    }
}
