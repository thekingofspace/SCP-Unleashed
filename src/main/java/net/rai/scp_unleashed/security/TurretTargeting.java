package net.rai.scp_unleashed.security;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlSession;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.stranger.StrangerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TurretTargeting {
    public static final ResourceLocation DClassId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dclass");

    private TurretTargeting() {
    }

    public static boolean isTarget(TurretRules Rules, @Nullable UUID Owner, LivingEntity Target) {
        if (!eligible(Target)) {
            return false;
        }
        if (Target instanceof Player User && Owner != null && Owner.equals(User.getUUID())) {
            return Boolean.TRUE.equals(Rules.nameVerdict(User.getGameProfile().getName()));
        }
        Boolean Listed = Rules.verdict(Target);
        if (Listed != null) {
            return Listed;
        }
        if (isFriendlyPet(Rules, Owner, Target)) {
            return false;
        }
        TurretCategory Category = categoryOf(Target);
        return Category != null && Rules.has(Category);
    }

    public static boolean eligible(LivingEntity Target) {
        if (!Target.isAlive() || Target.isDeadOrDying() || Target.isSpectator() || Target.isInvulnerable() || Target instanceof ArmorStand || Target instanceof StrangerEntity || !Targeting.isTargetable(Target)) {
            return false;
        }
        if (Target instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return false;
        }
        return !Scp268.isNeglected(Target);
    }

    private static boolean isFriendlyPet(TurretRules Rules, @Nullable UUID Owner, LivingEntity Target) {
        if (!(Target instanceof OwnableEntity Pet) || Pet.getOwnerUUID() == null) {
            return false;
        }
        if (Pet.getOwnerUUID().equals(Owner)) {
            return true;
        }
        LivingEntity Keeper = Pet.getOwner();
        return Keeper instanceof Player User && Rules.allowsName(User.getGameProfile().getName());
    }

    @Nullable
    public static TurretCategory categoryOf(Entity Target) {
        if (Target instanceof Player User) {
            if (ControlSessions.isControlling(User)) {
                ControlSession Session = ControlSessions.session(User);
                return DClassId.equals(Session.BodyType()) ? TurretCategory.DClass : TurretCategory.Personnel;
            }
            return TurretCategory.Players;
        }
        ResourceLocation Type = BuiltInRegistries.ENTITY_TYPE.getKey(Target.getType());
        if (ControlBodies.isPersonLike(Target)) {
            return DClassId.equals(Type) ? TurretCategory.DClass : TurretCategory.Personnel;
        }
        if (isAnomaly(Type)) {
            return TurretCategory.Scp;
        }
        if (Target instanceof Enemy) {
            return TurretCategory.Hostile;
        }
        if (Target instanceof Animal || Target instanceof WaterAnimal || Target instanceof AmbientCreature) {
            return TurretCategory.Animals;
        }
        return null;
    }

    public static boolean isAnomaly(ResourceLocation Type) {
        if (!Scp_unleashed.ModId.equals(Type.getNamespace())) {
            return false;
        }
        String Path = Type.getPath();
        return Path.startsWith("scp") || Path.startsWith("daevite") || Path.startsWith("z0001");
    }
}
