package net.rai.scp_unleashed.z0002;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record Z0002Spot(ResourceKey<Level> Dimension, Vec3 Pos, float Yaw, float Pitch) {
    public static Z0002Spot of(Entity Target) {
        return new Z0002Spot(Target.level().dimension(), Target.position(), Target.getYRot(), Target.getXRot());
    }

    public CompoundTag save() {
        CompoundTag Row = new CompoundTag();
        Row.putString("Dimension", Dimension.location().toString());
        Row.putDouble("X", Pos.x);
        Row.putDouble("Y", Pos.y);
        Row.putDouble("Z", Pos.z);
        Row.putFloat("Yaw", Yaw);
        Row.putFloat("Pitch", Pitch);
        return Row;
    }

    @Nullable
    public static Z0002Spot load(CompoundTag Row) {
        ResourceLocation Where = ResourceLocation.tryParse(Row.getString("Dimension"));
        if (Where == null) {
            return null;
        }
        return new Z0002Spot(ResourceKey.create(Registries.DIMENSION, Where), new Vec3(Row.getDouble("X"), Row.getDouble("Y"), Row.getDouble("Z")),
                Row.getFloat("Yaw"), Row.getFloat("Pitch"));
    }
}
