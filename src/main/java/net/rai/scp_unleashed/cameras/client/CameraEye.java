package net.rai.scp_unleashed.cameras.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CameraEye extends Entity {
    public CameraEye(Level World) {
        super(EntityType.MARKER, World);
        noPhysics = true;
        setInvisible(true);
        setNoGravity(true);
    }

    public void place(Vec3 Where, float Yaw, float Pitch) {
        setPos(Where.x, Where.y, Where.z);
        xo = Where.x;
        yo = Where.y;
        zo = Where.z;
        xOld = Where.x;
        yOld = Where.y;
        zOld = Where.z;
        setYRot(Yaw);
        setXRot(Pitch);
        yRotO = Yaw;
        xRotO = Pitch;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Root) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Root) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
