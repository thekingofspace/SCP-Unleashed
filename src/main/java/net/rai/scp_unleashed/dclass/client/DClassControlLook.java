package net.rai.scp_unleashed.dclass.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.client.ControlLook;
import net.rai.scp_unleashed.control.client.ControlPuppets;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.HumanoidPose;
import org.jetbrains.annotations.Nullable;

public class DClassControlLook implements ControlLook {
    public static final ModelLayerLocation ArmsLayer = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dclass_arms"), "main");
    @Nullable
    private static ModelPart Arms;

    public static LayerDefinition createArms() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f), PartPose.offset(-5.0f, 2.0f, 0.0f));
        Root.addOrReplaceChild("right_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.25f)),
                PartPose.offset(-5.0f, 2.0f, 0.0f));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f), PartPose.offset(5.0f, 2.0f, 0.0f));
        Root.addOrReplaceChild("left_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.25f)),
                PartPose.offset(5.0f, 2.0f, 0.0f));
        return LayerDefinition.create(Mesh, DClassSkins.Width, DClassSkins.Height);
    }

    private static ModelPart arms() {
        if (Arms == null) {
            Arms = Minecraft.getInstance().getEntityModels().bakeLayer(ArmsLayer);
        }
        return Arms;
    }

    public static void forgetModel() {
        Arms = null;
    }

    @Nullable
    @Override
    public LivingEntity createPuppet(ClientLevel World, CompoundTag Identity) {
        DClass Made = DClassRegistry.DClassType.get().create(World);
        if (Made == null) {
            return null;
        }
        Made.loadIdentity(Identity);
        Made.markPuppet();
        return Made;
    }

    @Override
    public void mirror(LivingEntity Puppet, AbstractClientPlayer User, CompoundTag Identity) {
        ControlPuppets.mirrorBasics(Puppet, User, Identity);
        if (!(Puppet instanceof DClassLike Body)) {
            return;
        }
        Body.Mirror.Using = User.isUsingItem();
        Body.Mirror.UseHand = User.getUsedItemHand();
        Body.Mirror.UseKind = User.isUsingItem() ? User.getUseItem().getUseAnimation() : UseAnim.NONE;
        Body.Mirror.UseTicks = User.getTicksUsingItem();
        Body.Mirror.Sitting = User.isPassenger();
        Body.Mirror.FallFlying = User.isFallFlying();
        Body.Mirror.Sleeping = User.isSleeping();
        Body.Mirror.Spinning = User.isAutoSpinAttack();
        Body.setPuppetOwner(User);
        Body.setLeftHanded(User.getMainArm() == HumanoidArm.LEFT);
        if (User.getSleepingPos().isPresent()) {
            Body.setSleepingPos(User.getSleepingPos().get());
        } else if (Body.getSleepingPos().isPresent()) {
            Body.clearSleepingPos();
        }
    }

    @Override
    public void mirrorHost(LivingEntity Puppet, LivingEntity Host, CompoundTag Identity) {
        ControlPuppets.mirrorBasics(Puppet, Host, Identity);
        if (Puppet instanceof DClassLike Body) {
            Body.markUndead();
            Body.markPuppet();
            Body.Mirror.Sitting = Host.isPassenger();
        }
    }

    @Override
    public void tickHosted(LivingEntity Puppet) {
        Puppet.tickCount++;
        if (Puppet instanceof DClassLike Body) {
            Body.tickLook();
        }
    }

    @Override
    public void tickPuppet(LivingEntity Puppet, AbstractClientPlayer User) {
        Puppet.tickCount++;
        if (Puppet instanceof DClassLike Body) {
            Body.tickLook();
        }
    }

    @Override
    public void copyPose(LivingEntity Puppet, AbstractClientPlayer User, PlayerModel<?> Model) {
        if (!(Puppet instanceof DClassLike Body)) {
            return;
        }
        HumanoidPose Mirror = Body.Mirror;
        part(Mirror, HumanoidPose.Head, Model.head, 0.0f, 0.0f);
        part(Mirror, HumanoidPose.Body, Model.body, 0.0f, 0.0f);
        part(Mirror, HumanoidPose.RightArm, Model.rightArm, -5.0f, 2.0f);
        part(Mirror, HumanoidPose.LeftArm, Model.leftArm, 5.0f, 2.0f);
        part(Mirror, HumanoidPose.RightLeg, Model.rightLeg, -1.9f, 12.0f);
        part(Mirror, HumanoidPose.LeftLeg, Model.leftLeg, 1.9f, 12.0f);
        Mirror.Copied = true;
    }

    @Override
    public void dropPose(LivingEntity Puppet) {
        if (Puppet instanceof DClassLike Body) {
            Body.Mirror.Copied = false;
        }
    }

    private static void part(HumanoidPose Mirror, int Index, ModelPart Part, float RestX, float RestY) {
        Mirror.copy(Index, Part.x - RestX, Part.y - RestY, Part.z, Part.xRot, Part.yRot, Part.zRot, Part.xScale, Part.yScale, Part.zScale);
    }

    @Override
    public boolean renderArm(PoseStack Matrices, MultiBufferSource Buffers, int Light, AbstractClientPlayer User, HumanoidArm Arm, CompoundTag Identity) {
        ResourceLocation Texture = texture(Identity);
        ModelPart Root = arms();
        boolean Right = Arm == HumanoidArm.RIGHT;
        ModelPart Limb = Root.getChild(Right ? "right_arm" : "left_arm");
        ModelPart Sleeve = Root.getChild(Right ? "right_sleeve" : "left_sleeve");
        Limb.resetPose();
        Sleeve.resetPose();
        Limb.zRot = Right ? 0.1f : -0.1f;
        Sleeve.zRot = Limb.zRot;
        Limb.render(Matrices, Buffers.getBuffer(RenderType.entitySolid(Texture)), Light, OverlayTexture.NO_OVERLAY);
        Sleeve.render(Matrices, Buffers.getBuffer(RenderType.entityTranslucent(Texture)), Light, OverlayTexture.NO_OVERLAY);
        return true;
    }

    @Override
    public ResourceLocation texture(CompoundTag Identity) {
        return DClassSkins.textureFor(Identity.getInt("Appearance"));
    }
}
