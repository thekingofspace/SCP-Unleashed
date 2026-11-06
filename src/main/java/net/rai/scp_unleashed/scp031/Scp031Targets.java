package net.rai.scp_unleashed.scp031;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp268.Scp268;

public final class Scp031Targets {
    private Scp031Targets() {
    }

    public static boolean isPerson(LivingEntity Candidate) {
        return Candidate instanceof Player || Candidate instanceof Villager || Candidate.getType().is(Scp031Registry.PersonLike);
    }

    public static boolean isValid(LivingEntity Candidate) {
        if (Candidate instanceof Scp031Mimic || Candidate instanceof Scp031Polyp || !Candidate.isAlive() || Candidate.isRemoved() || Candidate.isInvulnerable() || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        if (Candidate instanceof Player User) {
            return !User.isCreative() && !User.isSpectator() && !Scp268.isNeglected(User);
        }
        return Candidate instanceof Villager || Candidate.getType().is(Scp031Registry.PersonLike);
    }
}
