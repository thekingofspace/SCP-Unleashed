package net.rai.scp_unleashed.control.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface ControlLook {
    @Nullable
    LivingEntity createPuppet(ClientLevel World, CompoundTag Identity);

    default void mirror(LivingEntity Puppet, AbstractClientPlayer User, CompoundTag Identity) {
        ControlPuppets.mirrorBasics(Puppet, User, Identity);
    }

    default void tickPuppet(LivingEntity Puppet, AbstractClientPlayer User) {
        Puppet.tickCount++;
    }

    default void mirrorHost(LivingEntity Puppet, LivingEntity Host, CompoundTag Identity) {
        ControlPuppets.mirrorBasics(Puppet, Host, Identity);
    }

    default void tickHosted(LivingEntity Puppet) {
        Puppet.tickCount++;
    }

    default void copyPose(LivingEntity Puppet, AbstractClientPlayer User, PlayerModel<?> Model) {
    }

    default void dropPose(LivingEntity Puppet) {
    }

    boolean renderArm(PoseStack Pose, MultiBufferSource Buffers, int Light, AbstractClientPlayer User, HumanoidArm Arm, CompoundTag Identity);

    ResourceLocation texture(CompoundTag Identity);
}
