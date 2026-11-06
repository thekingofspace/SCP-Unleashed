package net.rai.scp_unleashed.playerclone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.CloningMachineBlockEntity;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassTrait;
import org.jetbrains.annotations.Nullable;

public final class PlayerCloneHatch implements CloningMachineBlockEntity.Hatch {
    @Nullable
    @Override
    public LivingEntity hatch(ServerLevel World, Vec3 At, float Yaw, int Appearance) {
        return hatch(World, At, Yaw, Appearance, new CompoundTag());
    }

    @Nullable
    @Override
    public LivingEntity hatch(ServerLevel World, Vec3 At, float Yaw, int Appearance, CompoundTag Sample) {
        PlayerClone Born = PlayerCloneRegistry.CloneType.get().create(World);
        if (Born == null) {
            return null;
        }
        Born.applySample(Sample);
        Born.identify(DClassTrait.random(World.getRandom()), DClassCrime.random(World.getRandom()), Appearance);
        Born.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Born.setYHeadRot(Yaw);
        Born.setYBodyRot(Yaw);
        Born.finalizeSpawn(World, World.getCurrentDifficultyAt(Born.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        if (!World.addFreshEntity(Born)) {
            return null;
        }
        Vec3 Ahead = At.add(Vec3.directionFromRotation(0.0f, Yaw).scale(2.5));
        Born.getNavigation().moveTo(Ahead.x, Ahead.y, Ahead.z, 0.6);
        return Born;
    }

    @Override
    public int appearance(ServerLevel World) {
        return 0;
    }
}
