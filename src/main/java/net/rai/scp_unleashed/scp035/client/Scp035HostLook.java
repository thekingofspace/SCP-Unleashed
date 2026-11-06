package net.rai.scp_unleashed.scp035.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.scp035.Scp035Host;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp035HostLook {
    public static final String DummyTag = "scp035_host_dummy";

    private Scp035HostLook() {
    }

    @Nullable
    public static LivingEntity dummyFor(Scp035Host Host) {
        if (Host.wasPlayer() || Host.getVictimType().isEmpty() || Host.dummyFailed() || !(Host.level() instanceof ClientLevel World)) {
            return null;
        }
        if (Host.dummy() instanceof LivingEntity Existing) {
            return Existing;
        }
        try {
            LivingEntity Made = create(World, Host);
            if (Made == null) {
                Host.markDummyFailed();
                return null;
            }
            Host.setDummy(Made);
            return Made;
        } catch (RuntimeException Problem) {
            Host.markDummyFailed();
            return null;
        }
    }

    @Nullable
    private static LivingEntity create(ClientLevel World, Scp035Host Host) {
        ResourceLocation Id = ResourceLocation.tryParse(Host.getVictimType());
        if (Id == null) {
            return null;
        }
        Optional<EntityType<?>> Type = BuiltInRegistries.ENTITY_TYPE.getOptional(Id);
        if (Type.isEmpty()) {
            return null;
        }
        Entity Made = Type.get().create(World);
        if (!(Made instanceof LivingEntity) || Made instanceof Player) {
            return null;
        }
        CompoundTag Look = Host.getVictimLook();
        if (!Look.isEmpty()) {
            try {
                Made.load(Look.copy());
            } catch (RuntimeException Problem) {
                Made = Type.get().create(World);
            }
        }
        if (!(Made instanceof LivingEntity Living)) {
            return null;
        }
        Living.addTag(DummyTag);
        Living.setCustomNameVisible(false);
        Living.setCustomName(null);
        Living.setInvisible(false);
        return Living;
    }

    public static void pose(Scp035Host Host, LivingEntity Dummy) {
        Dummy.setPos(Host.getX(), Host.getY(), Host.getZ());
        Dummy.xo = Host.xo;
        Dummy.yo = Host.yo;
        Dummy.zo = Host.zo;
        Dummy.xOld = Host.xOld;
        Dummy.yOld = Host.yOld;
        Dummy.zOld = Host.zOld;
        Dummy.setYRot(Host.getYRot());
        Dummy.yRotO = Host.yRotO;
        Dummy.setXRot(Host.getXRot());
        Dummy.xRotO = Host.xRotO;
        Dummy.yBodyRot = Host.yBodyRot;
        Dummy.yBodyRotO = Host.yBodyRotO;
        Dummy.yHeadRot = Host.yHeadRot;
        Dummy.yHeadRotO = Host.yHeadRotO;
        Dummy.attackAnim = Host.attackAnim;
        Dummy.oAttackAnim = Host.oAttackAnim;
        Dummy.swinging = Host.swinging;
        Dummy.swingTime = Host.swingTime;
        Dummy.hurtTime = Host.hurtTime;
        Dummy.deathTime = Host.deathTime;
        Dummy.tickCount = Host.tickCount;
        Dummy.setOnGround(Host.onGround());
        Dummy.setPose(Host.getPose());
        Dummy.setDeltaMovement(Host.getDeltaMovement());
        Dummy.setItemSlot(EquipmentSlot.HEAD, Host.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static void stride(Scp035Host Host, LivingEntity Dummy) {
        Dummy.walkAnimation.update(Host.walkAnimation.speed(), 1.0f);
    }
}
