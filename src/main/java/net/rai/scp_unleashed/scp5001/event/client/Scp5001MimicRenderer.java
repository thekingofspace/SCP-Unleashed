package net.rai.scp_unleashed.scp5001.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.playerclone.client.PlayerCloneSkins;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Mimic;

public class Scp5001MimicRenderer extends LivingEntityRenderer<Scp5001Mimic, PlayerModel<Scp5001Mimic>> {
    public static final float PlayerScale = 0.9375f;
    private final Scp5001MimicModel Wide;
    private final Scp5001MimicModel Slim;
    private final EntityRenderDispatcher Dispatcher;

    public Scp5001MimicRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp5001MimicModel(Context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        Wide = (Scp5001MimicModel) model;
        Slim = new Scp5001MimicModel(Context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        Dispatcher = Context.getEntityRenderDispatcher();
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(Context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(Context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), Context.getModelManager()));
        addLayer(new Scp5001MimicMarkLayer(this));
    }

    private PlayerSkin skin(Scp5001Mimic Mimic) {
        return PlayerCloneSkins.skinOf(Mimic.profile(), Mimic.getUUID());
    }

    @Override
    public ResourceLocation getTextureLocation(Scp5001Mimic Mimic) {
        return skin(Mimic).texture();
    }

    @Override
    public void render(Scp5001Mimic Mimic, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        if (Mimic.mimicsMob() && puppet(Mimic, Yaw, Partial, Pose, Buffers, Light)) {
            return;
        }
        model = skin(Mimic).model() == PlayerSkin.Model.SLIM ? Slim : Wide;
        super.render(Mimic, Yaw, Partial, Pose, Buffers, Light);
    }

    @Override
    protected void scale(Scp5001Mimic Mimic, PoseStack Pose, float Partial) {
        Pose.scale(PlayerScale * 0.95f, PlayerScale * 1.07f, PlayerScale * 0.95f);
    }

    @Override
    protected void setupRotations(Scp5001Mimic Mimic, PoseStack Pose, float Bob, float Yaw, float Partial, float Scale) {
        super.setupRotations(Mimic, Pose, Bob, Yaw, Partial, Scale);
        float Time = Mimic.tickCount + Partial + Mimic.getId() * 11.0f;
        float Lean = Mth.sin(Time * 0.07f) * 5.0f + (Mth.sin(Time * 0.53f) > 0.98f ? 14.0f : 0.0f);
        Pose.mulPose(Axis.ZP.rotationDegrees(Lean));
    }

    @Override
    protected boolean shouldShowName(Scp5001Mimic Mimic) {
        return Mimic.shouldShowName() && super.shouldShowName(Mimic);
    }

    private boolean puppet(Scp5001Mimic Mimic, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        EntityType<?> Type = Mimic.sourceType();
        if (Type == null) {
            return false;
        }
        LivingEntity Puppet = Mimic.Puppet;
        if (Puppet == null || Puppet.getType() != Type) {
            Entity Made;
            try {
                Made = Type.create(Mimic.level());
            } catch (RuntimeException Problem) {
                Made = null;
            }
            if (!(Made instanceof LivingEntity Living)) {
                return false;
            }
            Puppet = Living;
            Puppet.setSilent(true);
            Mimic.Puppet = Puppet;
        }
        Puppet.setPos(Mimic.getX(), Mimic.getY(), Mimic.getZ());
        Puppet.xo = Mimic.xo;
        Puppet.yo = Mimic.yo;
        Puppet.zo = Mimic.zo;
        Puppet.setYRot(Mimic.getYRot());
        Puppet.yRotO = Mimic.yRotO;
        Puppet.setXRot(Mimic.getXRot());
        Puppet.xRotO = Mimic.xRotO;
        Puppet.yBodyRot = Mimic.yBodyRot;
        Puppet.yBodyRotO = Mimic.yBodyRotO;
        Puppet.yHeadRot = Mimic.yHeadRot;
        Puppet.yHeadRotO = Mimic.yHeadRotO;
        Puppet.tickCount = Mimic.tickCount;
        Puppet.hurtTime = Mimic.hurtTime;
        Puppet.deathTime = Mimic.deathTime;
        Puppet.setOnGround(Mimic.onGround());
        EntityRenderer<? super LivingEntity> Renderer = Dispatcher.getRenderer(Puppet);
        if (Renderer == null) {
            return false;
        }
        float Time = Mimic.tickCount + Partial + Mimic.getId() * 11.0f;
        Pose.pushPose();
        Pose.scale(0.94f, 1.16f, 0.94f);
        Pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(Time * 0.09f) * 7.0f + (Mth.sin(Time * 0.41f) > 0.97f ? 18.0f : 0.0f)));
        Pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(Time * 0.05f) * 4.0f));
        try {
            Renderer.render(Puppet, Yaw, Partial, Pose, Buffers, Light);
        } catch (RuntimeException Problem) {
            Pose.popPose();
            return false;
        }
        Pose.popPose();
        return true;
    }
}
