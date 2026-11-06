package net.rai.scp_unleashed.scp027;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;

public interface Scp027Vermin {
    Scp027Leash leash();

    default Mob body() {
        return (Mob) this;
    }

    default boolean flies() {
        return false;
    }

    static boolean isPrey(Scp027Vermin Vermin, LivingEntity Target) {
        if (Target instanceof Scp027Vermin || Target instanceof Scp027Entity || !Target.isAlive() || !Targeting.isTargetable(Target)) {
            return false;
        }
        if (!(Target instanceof Player) && !ControlBodies.isPersonLike(Target)) {
            return false;
        }
        Scp027Entity Master = Vermin.leash().master();
        return Master != null && Target.distanceToSqr(Master) <= Scp027Leash.LeashRange * Scp027Leash.LeashRange;
    }
}
