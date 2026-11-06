package net.rai.scp_unleashed.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabExposure;

import java.util.List;
import java.util.function.Predicate;

public final class ControlBodies {
    public static final String LookKey = "Look";
    public static final String IdentityKey = "Identity";
    public static final TagKey<EntityType<?>> PersonLike = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "person_like"));
    public static final List<String> TransientKeys = List.of("Brain", "Attributes", "Motion", "Pos", "Rotation", "Passengers", "Leash");

    private ControlBodies() {
    }

    public static CompoundTag lookOf(LivingEntity Body) {
        CompoundTag Saved = new CompoundTag();
        if (Body instanceof Controllable Person) {
            CompoundTag Identity = Person.saveIdentity();
            Identity.putString("ControlName", Person.controlName().getString());
            Saved.putString(LookKey, Person.controlLook().toString());
            Saved.put(IdentityKey, Identity);
        } else if (isPersonLike(Body)) {
            CompoundTag Identity = Body.saveWithoutId(new CompoundTag());
            for (String Key : TransientKeys) {
                Identity.remove(Key);
            }
            Saved.putString(LookKey, BuiltInRegistries.ENTITY_TYPE.getKey(Body.getType()).toString());
            Saved.put(IdentityKey, Identity);
        }
        return Saved;
    }

    public static boolean isPersonLike(Entity Target) {
        return Target instanceof Controllable || Target.getType().is(PersonLike);
    }

    public static List<LivingEntity> inSight(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius, Predicate<LivingEntity> Candidate) {
        AABB Reach = AABB.ofSize(Source, Radius * 2.0, Radius * 2.0, Radius * 2.0);
        return World.getEntitiesOfClass(LivingEntity.class, Reach, Target -> isPersonLike(Target) && Target.isAlive() && Target.getEyePosition().distanceToSqr(Source) <= Radius * Radius
                && Candidate.test(Target) && LabExposure.sees(World, Source, Target, Ignored));
    }
}
