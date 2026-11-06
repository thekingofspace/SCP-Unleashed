package net.rai.scp_unleashed.playerclone.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.ResolvableProfile;
import net.rai.scp_unleashed.dclass.client.DClassControlLook;
import net.rai.scp_unleashed.playerclone.PlayerClone;
import net.rai.scp_unleashed.playerclone.PlayerCloneRegistry;
import net.rai.scp_unleashed.playerclone.PlayerSamples;
import org.jetbrains.annotations.Nullable;

public class PlayerCloneControlLook extends DClassControlLook {
    @Nullable
    private static PlayerModel<LivingEntity> Wide;
    @Nullable
    private static PlayerModel<LivingEntity> Slim;
    @Nullable
    private static CompoundTag LastIdentity;
    @Nullable
    private static ResolvableProfile LastProfile;

    private static PlayerModel<LivingEntity> arms(boolean Thin) {
        if (Thin) {
            if (Slim == null) {
                Slim = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
            }
            return Slim;
        }
        if (Wide == null) {
            Wide = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        return Wide;
    }

    public static void forgetArms() {
        Wide = null;
        Slim = null;
    }

    private static PlayerSkin skinOf(CompoundTag Identity) {
        if (Identity != LastIdentity) {
            LastIdentity = Identity;
            LastProfile = PlayerSamples.read(Identity.getCompound(PlayerClone.SampleKey));
        }
        return PlayerCloneSkins.skinOf(LastProfile, Util.NIL_UUID);
    }

    @Nullable
    @Override
    public LivingEntity createPuppet(ClientLevel World, CompoundTag Identity) {
        PlayerClone Made = PlayerCloneRegistry.CloneType.get().create(World);
        if (Made == null) {
            return null;
        }
        Made.loadIdentity(Identity);
        Made.markPuppet();
        return Made;
    }

    @Override
    public boolean renderArm(PoseStack Matrices, MultiBufferSource Buffers, int Light, AbstractClientPlayer User, HumanoidArm Arm, CompoundTag Identity) {
        PlayerSkin Skin = skinOf(Identity);
        PlayerModel<LivingEntity> Model = arms(Skin.model() == PlayerSkin.Model.SLIM);
        boolean Right = Arm == HumanoidArm.RIGHT;
        ModelPart Limb = Right ? Model.rightArm : Model.leftArm;
        ModelPart Sleeve = Right ? Model.rightSleeve : Model.leftSleeve;
        Limb.resetPose();
        Sleeve.resetPose();
        Limb.visible = true;
        Sleeve.visible = true;
        Limb.zRot = Right ? 0.1f : -0.1f;
        Sleeve.zRot = Limb.zRot;
        ResourceLocation Texture = Skin.texture();
        Limb.render(Matrices, Buffers.getBuffer(RenderType.entitySolid(Texture)), Light, OverlayTexture.NO_OVERLAY);
        Sleeve.render(Matrices, Buffers.getBuffer(RenderType.entityTranslucent(Texture)), Light, OverlayTexture.NO_OVERLAY);
        return true;
    }

    @Override
    public ResourceLocation texture(CompoundTag Identity) {
        return skinOf(Identity).texture();
    }
}
