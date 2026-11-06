package net.rai.scp_unleashed.scp031.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp031.Scp031Mimic;

public class Scp031GuiseRenderer extends LivingEntityRenderer<Scp031Mimic, Scp031GuiseModel> {
    public static final float PlayerScale = 0.9375f;
    public static final float ShadowSize = 0.5f;
    public static final double NameRange = 64.0;
    public static final double DiscreteNameRange = 32.0;
    public static final double PlayerHeight = 1.8;
    public static final double PlayerCrouchHeight = 1.5;

    private final Scp031GuiseModel Wide;
    private final Scp031GuiseModel Slim;
    private Scp031Disguises.Disguise Current = Scp031Disguises.Fallback;

    public Scp031GuiseRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp031GuiseModel(Context.bakeLayer(ModelLayers.PLAYER), false), ShadowSize);
        this.Wide = model;
        this.Slim = new Scp031GuiseModel(Context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(Scp031Mimic Mimic, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        Current = Scp031Disguises.of(Mimic);
        model = Current.Slim() ? Slim : Wide;
        model.crouching = Mimic.isCrouching();
        model.setAllVisible(true);
        super.render(Mimic, Yaw, PartialTick, Pose, Buffers, PackedLight);
        if (Current.copiesPlayer() && Current.Name() != null && nameVisible(Mimic)) {
            Pose.pushPose();
            Pose.translate(0.0, (Mimic.isCrouching() ? PlayerCrouchHeight : PlayerHeight) - Mimic.getBbHeight(), 0.0);
            renderNameTag(Mimic, Current.Name(), Pose, Buffers, PackedLight, PartialTick);
            Pose.popPose();
        }
    }

    private boolean nameVisible(Scp031Mimic Mimic) {
        if (Mimic.isInvisible() || Mimic.isVehicle() || !Minecraft.renderNames()) {
            return false;
        }
        double Range = Mimic.isDiscrete() ? DiscreteNameRange : NameRange;
        return entityRenderDispatcher.distanceToSqr(Mimic) < Range * Range;
    }

    @Override
    public ResourceLocation getTextureLocation(Scp031Mimic Mimic) {
        return Current.Texture();
    }

    @Override
    public Vec3 getRenderOffset(Scp031Mimic Mimic, float PartialTick) {
        return Mimic.isCrouching() ? new Vec3(0.0, Mimic.getScale() * -2.0 / 16.0, 0.0) : Vec3.ZERO;
    }

    @Override
    protected void scale(Scp031Mimic Mimic, PoseStack Pose, float PartialTick) {
        Pose.scale(PlayerScale, PlayerScale, PlayerScale);
    }

    @Override
    protected boolean shouldShowName(Scp031Mimic Mimic) {
        return false;
    }
}
