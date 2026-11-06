package net.rai.scp_unleashed.scp5001.mekhane.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.mekhane.entity.Mekhane;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

import java.util.Optional;

public class MekhaneRenderer extends GeoEntityRenderer<Mekhane> {
    public static final Vector3f Palm = new Vector3f(-10.5f / 16.0f, 41.0f / 16.0f, 1.5f / 16.0f);
    public static final Vector3f AxeEdge = new Vector3f(32.0f / 16.0f, 31.0f / 16.0f, -38.0f / 16.0f);
    public static final float MajorRadius = 8.6f;
    public static final float MinorRadius = 2.6f;
    public static final float OrbitRadius = 7.6f;
    public static final float PortalRadius = 26.0f;
    public static final float TiltPivot = Mekhane.TiltPivot;
    public static final int SkyLight = 15;
    private Vec3 Hand = Vec3.ZERO;
    private boolean HandKnown;

    public MekhaneRenderer(EntityRendererProvider.Context Context) {
        super(Context, new MekhaneGeoModel());
        addRenderLayer(new MekhaneGlowLayer(this));
        withScale(Mekhane.RenderScale);
        shadowRadius = 0.0f;
    }

    @Override
    public boolean shouldRender(Mekhane Owner, Frustum View, double X, double Y, double Z) {
        return View.isVisible(Owner.getBoundingBoxForCulling());
    }

    @Override
    public void preRender(PoseStack Pose, Mekhane Owner, BakedGeoModel Model, @Nullable MultiBufferSource Buffers, @Nullable VertexConsumer Buffer, boolean ReRender, float Partial,
                          int Light, int Overlay, int Colour) {
        if (!ReRender) {
            super.preRender(Pose, Owner, Model, Buffers, Buffer, false, Partial, Light, Overlay, Colour);
        }
    }

    @Override
    protected int getSkyLightLevel(Mekhane Owner, BlockPos Pos) {
        return SkyLight;
    }

    @Override
    public RenderType getRenderType(Mekhane Owner, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float Partial) {
        return Owner.alpha(Partial) < 0.99f ? RenderType.entityTranslucent(Texture) : super.getRenderType(Owner, Texture, Buffers, Partial);
    }

    @Override
    public Color getRenderColor(Mekhane Owner, float Partial, int PackedLight) {
        float Alpha = Owner.alpha(Partial);
        if (Alpha >= 0.99f) {
            return super.getRenderColor(Owner, Partial, PackedLight);
        }
        int Lift = (int) (255 * Mth.clamp(1.0f - Alpha, 0.0f, 1.0f) * 0.6f);
        return Color.ofARGB(Mth.clamp((int) (Alpha * 255.0f), 0, 255), 255, Math.min(255, 220 + Lift / 8), Math.min(255, 170 + Lift / 3));
    }

    @Override
    public void renderFinal(PoseStack Pose, Mekhane Owner, BakedGeoModel Model, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer, float Partial, int Light,
                            int Overlay, int Colour) {
        super.renderFinal(Pose, Owner, Model, Buffers, Buffer, Partial, Light, Overlay, Colour);
        Optional<GeoBone> Found = Model.getBone("hand_ll");
        if (Found.isPresent() && Found.get().isTrackingMatrices()) {
            Matrix4f Local = new Matrix4f(Found.get().getLocalSpaceMatrix());
            Vector3f Point = Local.transformPosition(new Vector3f(Palm));
            Hand = new Vec3(Point.x, Point.y, Point.z);
            HandKnown = true;
        }
    }

    @Override
    public void render(Mekhane Owner, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        boolean Shadow = ShaderCompat.renderingShadowPass();
        if (!Shadow) {
            Owner.drawn(MekhaneSkyPass.frame());
        }
        float Alpha = Owner.alpha(Partial);
        if (Alpha > Mekhane.Faded) {
            super.render(Owner, Yaw, Partial, Pose, Buffers, Light);
        }
        if (Shadow) {
            return;
        }
        float Time = Owner.tickCount + Partial;
        Vec3 Origin = Owner.getPosition(Partial);
        Vec3 Fallback = fallbackHand(Owner, Partial);
        Vec3 Sigil = HandKnown ? Hand : Fallback;
        float Charge = Owner.charge(Partial) * Alpha;
        if (Charge > 0.01f) {
            Vec3 Facing = Vec3.directionFromRotation(0.0f, Owner.yaw(Partial));
            float Size = Math.min(1.4f, Charge);
            MekhaneLight.disc(Buffers, Pose, MekhaneLight.Major, Sigil, Facing.add(0.0, 0.25, 0.0), MajorRadius * Size, Time * 0.03f, 1.0f, 0.9f, 0.62f, Math.min(1.0f, Charge));
            MekhaneLight.disc(Buffers, Pose, MekhaneLight.Major, Sigil, new Vec3(0.0, 1.0, 0.2), MajorRadius * 0.7f * Size, -Time * 0.05f, 1.0f, 0.86f, 0.55f, Math.min(1.0f, Charge) * 0.6f);
            for (int Index = 0; Index < 3; Index++) {
                float Theta = Time * (0.05f + 0.012f * Index) + Index * Mth.TWO_PI / 3.0f;
                Vec3 Around = new Vec3(Mth.cos(Theta) * OrbitRadius * Size, Mth.sin(Theta * 1.3f) * OrbitRadius * 0.6f * Size, Mth.sin(Theta) * OrbitRadius * Size);
                MekhaneLight.disc(Buffers, Pose, MekhaneLight.Minor, Sigil.add(Around), Facing.add(Around.normalize().scale(0.4)), MinorRadius * Size, Time * 0.09f, 1.0f, 0.92f, 0.7f,
                        Math.min(1.0f, Charge));
            }
        }
        float Laser = Owner.laser(Partial);
        Vec3 Target = Owner.target().subtract(Origin);
        if (Laser > 0.01f) {
            float Throb = 1.0f + 0.12f * Mth.sin(Time * 0.9f);
            MekhaneLight.ray(Buffers, Pose, Sigil, Target, 3.2f * Laser * Throb, Time * 0.08f, -Time * 0.12f, 0.95f * Laser, 0.85f * Laser);
            MekhaneLight.ray(Buffers, Pose, Sigil, Target, 6.5f * Laser * Throb, -Time * 0.05f, -Time * 0.07f, 0.35f * Laser, 0.25f * Laser);
            MekhaneLight.disc(Buffers, Pose, MekhaneLight.ShockRing, Target, Target.subtract(Sigil), 9.0f * Laser * Throb, Time * 0.2f, 1.0f, 0.9f, 0.6f, 0.7f * Laser);
        }
        float Open = Owner.portalOpen(Partial);
        if (Open > 0.01f) {
            Vec3 Gate = Owner.portal().subtract(Origin);
            Vec3 Normal = Gate.subtract(new Vec3(0.0, 33.0, 0.0));
            float Radius = PortalRadius * Open;
            MekhaneLight.disc(Buffers, Pose, MekhaneLight.PortalCore, Gate, Normal, Radius * 0.92f, -Time * 0.04f, 1.0f, 0.95f, 0.85f, Open);
            MekhaneLight.disc(Buffers, Pose, MekhaneLight.Major, Gate, Normal, Radius * 1.12f, Time * 0.02f, 1.0f, 0.88f, 0.55f, Open);
            MekhaneLight.disc(Buffers, Pose, MekhaneLight.Major, Gate, Normal, Radius * 1.4f, -Time * 0.015f, 1.0f, 0.8f, 0.45f, 0.55f * Open);
            for (int Index = 0; Index < 8; Index++) {
                float Theta = Index * Mth.TWO_PI / 8.0f + Time * 0.01f;
                Vec3 Helper = Math.abs(Normal.normalize().y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
                Vec3 First = Normal.normalize().cross(Helper).normalize();
                Vec3 Second = Normal.normalize().cross(First).normalize();
                Vec3 Out = First.scale(Mth.cos(Theta)).add(Second.scale(Mth.sin(Theta)));
                MekhaneLight.ray(Buffers, Pose, Gate.add(Out.scale(Radius * 1.1f)), Gate.add(Out.scale(Radius * (1.6f + 0.2f * Mth.sin(Time * 0.3f + Index)))), 0.9f * Open, 0.0f,
                        -Time * 0.1f, 0.8f * Open, 0.0f);
            }
        }
    }

    private static Vec3 fallbackHand(Mekhane Owner, float Partial) {
        float Radians = Owner.yaw(Partial) * Mth.DEG_TO_RAD;
        Vec3 Ahead = new Vec3(-Mth.sin(Radians), 0.0, Mth.cos(Radians));
        Vec3 Left = new Vec3(Ahead.z, 0.0, -Ahead.x);
        return Ahead.scale(9.0).add(Left.scale(9.0)).add(0.0, 30.0, 0.0);
    }

    @Override
    protected void applyRotations(Mekhane Owner, PoseStack Pose, float Age, float Yaw, float Partial, float NativeScale) {
        super.applyRotations(Owner, Pose, Age, Owner.yaw(Partial), Partial, NativeScale);
        float Dive = Owner.dive(Partial);
        float Bank = Owner.bank(Partial);
        if (Math.abs(Dive) < 0.01f && Math.abs(Bank) < 0.01f) {
            return;
        }
        Pose.translate(0.0f, TiltPivot, 0.0f);
        Pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-Dive));
        Pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-Bank));
        Pose.translate(0.0f, -TiltPivot, 0.0f);
    }

    @Override
    protected float getDeathMaxRotation(Mekhane Owner) {
        return 0.0f;
    }
}
