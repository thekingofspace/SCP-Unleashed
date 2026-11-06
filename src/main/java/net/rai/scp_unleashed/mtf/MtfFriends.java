package net.rai.scp_unleashed.mtf;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.security.TurretTargeting;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

public final class MtfFriends {
    public static final TagKey<EntityType<?>> HoldFire = TagKey.create(Registries.ENTITY_TYPE, MtfRegistry.location("mtf_hold_fire"));

    private MtfFriends() {
    }

    public static boolean isFriend(UUID CallerId, @Nullable Entity Other) {
        if (Other == null) {
            return false;
        }
        if (CallerId.equals(Other.getUUID())) {
            return true;
        }
        if (Other instanceof MtfOperative || Other instanceof MtfHelicopter) {
            return true;
        }
        if (Other instanceof OwnableEntity Pet && CallerId.equals(Pet.getOwnerUUID())) {
            return true;
        }
        if (Other instanceof HostileRaider) {
            return false;
        }
        if (Other instanceof DClassLike Person) {
            return !threatens(CallerId, Person);
        }
        if (Other instanceof Player User && User.getServer() != null) {
            Player Caller = User.getServer().getPlayerList().getPlayer(CallerId);
            return Caller != null && Caller.isAlliedTo(User);
        }
        return false;
    }

    public static boolean threatens(UUID CallerId, Entity Other) {
        if (!(Other instanceof Mob Hunter)) {
            return false;
        }
        LivingEntity Prey = Hunter.getTarget();
        if (Prey == null || !Prey.isAlive()) {
            return false;
        }
        return CallerId.equals(Prey.getUUID()) || Prey instanceof MtfOperative || Prey instanceof OwnableEntity Pet && CallerId.equals(Pet.getOwnerUUID());
    }

    public static boolean isBystander(Entity Other) {
        return Other instanceof Player || Other instanceof AbstractVillager || Other instanceof IronGolem || Other instanceof ArmorStand;
    }

    public static Predicate<Entity> shotGuard(UUID CallerId, @Nullable LivingEntity Target) {
        return Found -> Found != Target && (isFriend(CallerId, Found) || isBystander(Found));
    }

    public static boolean isAnomaly(Entity Other) {
        return TurretTargeting.isAnomaly(BuiltInRegistries.ENTITY_TYPE.getKey(Other.getType()));
    }

    public static boolean isHostile(UUID CallerId, LivingEntity Other) {
        if (!Other.isAlive() || Other instanceof ArmorStand || !Targeting.isTargetable(Other) || isFriend(CallerId, Other)) {
            return false;
        }
        if (threatens(CallerId, Other)) {
            return true;
        }
        if (Other.getType().is(HoldFire)) {
            return false;
        }
        if (Other instanceof NeutralMob Neutral && !Neutral.isAngry()) {
            return false;
        }
        if (Other instanceof Enemy) {
            return true;
        }
        return isAnomaly(Other) && Other instanceof Mob Hunter && Hunter.getTarget() instanceof Player;
    }

    public static boolean canEngage(UUID CallerId, LivingEntity Other, boolean Explicit) {
        if (!Other.isAlive() || Other.isRemoved() || Other instanceof ArmorStand || !Targeting.isTargetable(Other) || isFriend(CallerId, Other)) {
            return false;
        }
        if (Other instanceof Player User) {
            if (User.isSpectator() || User.isCreative() || !Explicit) {
                return false;
            }
            return User.getServer() != null && User.getServer().isPvpAllowed();
        }
        return Explicit || isHostile(CallerId, Other);
    }
}
