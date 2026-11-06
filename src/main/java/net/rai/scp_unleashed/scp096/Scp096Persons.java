package net.rai.scp_unleashed.scp096;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlSession;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.goggles.Goggles;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Scp096Persons {
    private Scp096Persons() {
    }

    public static boolean shielded(LivingEntity Viewer) {
        return Goggles.isWearing(Viewer, Goggles.Kind.Scramble);
    }

    public static boolean isWitness(@Nullable Entity Found) {
        if (!(Found instanceof LivingEntity Body) || Body instanceof Scp096ShyGuy || !Body.isAlive() || Body.isRemoved() || Body.isSpectator() || !Targeting.canWitness(Body)) {
            return false;
        }
        if (Body instanceof Player User) {
            return !User.isCreative() && !Scp268.isNeglected(User);
        }
        if (!ControlBodies.isPersonLike(Body) || ControlSessions.isBoundToSession(Body)) {
            return false;
        }
        return !(Body instanceof DClassLike Person) || !Person.isSpecimen() && !Person.isPuppet();
    }

    @Nullable
    public static LivingEntity attacker(DamageSource Source, LivingEntity Victim) {
        Entity Cause = Source.getEntity();
        if (!(Cause instanceof LivingEntity Body) || Body == Victim || Body instanceof Scp096ShyGuy || !Body.isAlive() || Body.isRemoved() || Body.isSpectator() || !Targeting.isTargetable(Body)) {
            return null;
        }
        if (Body instanceof Player User && (User.isCreative() || Scp268.isNeglected(User))) {
            return null;
        }
        return Body;
    }

    public static boolean canWatch(LivingEntity Viewer) {
        return Targeting.canWitness(Viewer) && !Viewer.isSleeping() && !(Viewer instanceof DClassLike Person && Person.eyesShut()) && !Viewer.hasEffect(MobEffects.BLINDNESS) && !shielded(Viewer)
                && !Senses.isBlindfolded(Viewer);
    }

    public static UUID keyOf(LivingEntity Viewer) {
        if (Viewer instanceof ServerPlayer User && ControlSessions.isControlling(User)) {
            ControlSession Session = ControlSessions.session(User);
            if (Session.active()) {
                return Session.BodyId();
            }
        }
        return Viewer.getUUID();
    }

    public static boolean isKey(UUID Key, LivingEntity Body) {
        return Key.equals(keyOf(Body));
    }

    @Nullable
    public static ServerPlayer controllerOf(MinecraftServer Server, UUID BodyId) {
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            if (ControlSessions.isControlling(User) && BodyId.equals(ControlSessions.session(User).BodyId())) {
                return User;
            }
        }
        return null;
    }

    @Nullable
    public static LivingEntity resolve(ServerLevel World, UUID Key) {
        ServerPlayer Controller = controllerOf(World.getServer(), Key);
        if (Controller != null) {
            return Controller.level() == World ? Controller : null;
        }
        ServerPlayer Online = World.getServer().getPlayerList().getPlayer(Key);
        if (Online != null) {
            if (Online.level() != World || ControlSessions.isControlling(Online)) {
                return null;
            }
            return Online;
        }
        Entity Found = World.getEntity(Key);
        return Found instanceof LivingEntity Body ? Body : null;
    }

    public static boolean elsewhere(ServerLevel World, UUID Key) {
        ServerPlayer Controller = controllerOf(World.getServer(), Key);
        if (Controller != null) {
            return Controller.level() != World;
        }
        ServerPlayer Online = World.getServer().getPlayerList().getPlayer(Key);
        if (Online != null) {
            return Online.level() != World && !ControlSessions.isControlling(Online);
        }
        for (ServerLevel Other : World.getServer().getAllLevels()) {
            if (Other != World && Other.getEntity(Key) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean huntable(LivingEntity Body) {
        if (!Body.isAlive() || Body.isRemoved() || Body.isDeadOrDying() || Body.isSpectator() || !Targeting.isTargetable(Body)) {
            return false;
        }
        if (Body instanceof Player User) {
            return !User.isCreative() && User.level().getDifficulty() != Difficulty.PEACEFUL && !Scp268.isNeglected(User);
        }
        return true;
    }
}
