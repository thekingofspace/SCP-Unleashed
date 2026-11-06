package net.rai.scp_unleashed.control;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public interface Controllable {
    CompoundTag saveBody(HolderLookup.Provider Registries);

    void loadBody(CompoundTag Body, HolderLookup.Provider Registries);

    CompoundTag saveIdentity();

    void loadIdentity(CompoundTag Identity);

    Component controlName();

    int controlIncarnation();

    void setControlIncarnation(int Incarnation);

    default boolean canBeControlledBy(ServerPlayer User) {
        return controlBody().isAlive() && !controlBody().isRemoved();
    }

    default void beforeControl(ServerPlayer User) {
    }

    default ResourceLocation controlLook() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(controlBody().getType());
    }

    default LivingEntity controlBody() {
        return (LivingEntity) this;
    }
}
