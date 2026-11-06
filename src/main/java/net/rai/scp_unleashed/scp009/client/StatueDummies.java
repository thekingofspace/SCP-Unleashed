package net.rai.scp_unleashed.scp009.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp009.Scp009Statue;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class StatueDummies {
    public static final String DummyTag = "scp009_statue_dummy";

    private StatueDummies() {
    }

    @Nullable
    public static Entity dummyFor(Scp009Statue Statue) {
        if (Statue.dummyFailed() || !(Statue.level() instanceof ClientLevel World)) {
            return null;
        }
        Entity Existing = Statue.dummy();
        if (Existing != null) {
            return Existing;
        }
        try {
            Entity Made = create(World, Statue);
            if (Made == null) {
                Statue.markDummyFailed();
                return null;
            }
            pose(Statue, Made);
            Statue.setDummy(Made);
            return Made;
        } catch (RuntimeException Problem) {
            Statue.markDummyFailed();
            return null;
        }
    }

    @Nullable
    private static Entity create(ClientLevel World, Scp009Statue Statue) {
        if (Statue.isPlayerShaped()) {
            GameProfile Profile = Statue.profile().orElseGet(() -> new GameProfile(UUID.nameUUIDFromBytes(Statue.getStringUUID().getBytes()), "Statue"));
            StatuePlayer Body = new StatuePlayer(World, Profile);
            CompoundTag Worn = Statue.equipment();
            for (EquipmentSlot Slot : EquipmentSlot.values()) {
                if (Worn.contains(Slot.getName())) {
                    Body.setItemSlot(Slot, ItemStack.parseOptional(World.registryAccess(), Worn.getCompound(Slot.getName())));
                }
            }
            return Body;
        }
        Optional<EntityType<?>> Type = BuiltInRegistries.ENTITY_TYPE.getOptional(Statue.sourceType());
        if (Type.isEmpty()) {
            return null;
        }
        Entity Made = Type.get().create(World);
        if (!(Made instanceof LivingEntity)) {
            return null;
        }
        if (!Statue.snapshot().isEmpty()) {
            try {
                Made.load(Statue.snapshot().copy());
            } catch (RuntimeException Problem) {
                Made = Type.get().create(World);
            }
        }
        return Made;
    }

    private static void pose(Scp009Statue Statue, Entity Made) {
        Made.setPos(Statue.getX(), Statue.getY(), Statue.getZ());
        Made.setYRot(Statue.bodyYaw());
        Made.yRotO = Statue.bodyYaw();
        Made.setXRot(Statue.headPitch());
        Made.xRotO = Statue.headPitch();
        Made.setInvisible(false);
        Made.setCustomNameVisible(false);
        Made.setRemainingFireTicks(0);
        Made.addTag(DummyTag);
        if (Made instanceof LivingEntity Living) {
            Living.yBodyRot = Statue.bodyYaw();
            Living.yBodyRotO = Statue.bodyYaw();
            Living.yHeadRot = Statue.headYaw();
            Living.yHeadRotO = Statue.headYaw();
            Living.walkAnimation.setSpeed(Statue.limbPosition());
            Living.walkAnimation.update(Statue.limbPosition(), 1.0f);
            Living.walkAnimation.setSpeed(Statue.limbSpeed());
            Living.attackAnim = Statue.swing();
            Living.oAttackAnim = Statue.swing();
            Living.hurtTime = 0;
            Living.deathTime = 0;
            Living.setTicksFrozen(0);
            Living.setPose(Statue.crouching() ? Pose.CROUCHING : Pose.STANDING);
            Living.tickCount = Statue.age();
        }
    }
}
