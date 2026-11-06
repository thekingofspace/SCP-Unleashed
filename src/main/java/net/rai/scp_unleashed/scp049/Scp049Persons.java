package net.rai.scp_unleashed.scp049;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp008.Scp008Zombie;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

public final class Scp049Persons {
    private Scp049Persons() {
    }

    public static boolean sparesPlayers(Entity Viewer) {
        return Viewer.level().getDifficulty() == Difficulty.PEACEFUL;
    }

    public static boolean isPerson(@Nullable Entity Found, boolean SparePlayers) {
        if (!(Found instanceof LivingEntity Body) || Body instanceof Scp049Cured || Body instanceof Scp049Doctor || InfectionImmunity.isImmune(Body)) {
            return false;
        }
        if (!Body.isAlive() || Body.isRemoved() || Body.isDeadOrDying() || Body.isSpectator()) {
            return false;
        }
        if (Body instanceof Player User) {
            return !SparePlayers && !User.isCreative() && !Scp268.isNeglected(User);
        }
        return Body.getType().is(Scp049Registry.PersonLike);
    }

    public static boolean isPatient(@Nullable Entity Found) {
        return Found instanceof Villager Patient && Patient.isAlive() && !Patient.isRemoved() && !Patient.isBaby() && Targeting.isTargetable(Patient);
    }

    public static boolean isOrdinaryZombie(@Nullable Entity Found) {
        if (!(Found instanceof Mob Body) || Body instanceof Scp049Cured || !Body.isAlive() || Body.isRemoved() || !(Body instanceof Enemy)) {
            return false;
        }
        if (Scp_unleashed.ModId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(Body.getType()).getNamespace())) {
            return false;
        }
        return Body instanceof Zombie || Body.getType().is(EntityTypeTags.ZOMBIES);
    }

    public static boolean isPlagueZombie(@Nullable Entity Found) {
        return Found instanceof Scp008Zombie Plague && Plague.isAlive() && !Plague.isRemoved();
    }

    public static boolean isZombieFoe(@Nullable Entity Found) {
        return isOrdinaryZombie(Found) || isPlagueZombie(Found);
    }

    public static boolean isNeutralZombie(Entity Found) {
        return Found instanceof ZombifiedPiglin;
    }

    public static boolean isNoticeable(LivingEntity Target) {
        return !Target.isInvisible() || Target.hasEffect(MobEffects.GLOWING);
    }
}
