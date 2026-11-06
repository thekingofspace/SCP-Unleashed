package net.rai.scp_unleashed.health;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class LethalDamage {
    public static final float Floor = 1.0e6f;
    public static final float Headroom = 100.0f;
    public static final float Ceiling = 1.0e30f;

    private LethalDamage() {
    }

    public static float against(LivingEntity Victim) {
        float Pool = Victim.getMaxHealth() + Math.max(0.0f, Victim.getAbsorptionAmount());
        if (!Float.isFinite(Pool)) {
            return Floor;
        }
        return Mth.clamp(Pool * Headroom, Floor, Ceiling);
    }
}
