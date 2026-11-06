package net.rai.scp_unleashed.scp538.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp538.Scp538Spider;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.util.Color;

public class Scp538Renderer extends SharedGeoRenderer<Scp538Spider> {
    public static final float Scale = 0.2f;
    public static final float TurnRate = 0.4f;
    public static final float FadeFloor = 0.15f;
    public static final double SnapGap = 20.0;

    public Scp538Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp538GeoModel());
        withScale(Scale);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp538Spider Spider, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Spider.setRenderPartial(PartialTick);
        super.render(Spider, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    public Color getRenderColor(Scp538Spider Spider, float PartialTick, int PackedLight) {
        float Alpha = 1.0f - (1.0f - FadeFloor) * Spider.fadeShare();
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, Alpha);
    }

    public static Vector3f look(float XRot, float YRot) {
        float Pitch = XRot * Mth.DEG_TO_RAD;
        float Turn = -YRot * Mth.DEG_TO_RAD;
        float Flat = Mth.cos(Pitch);
        return new Vector3f(Mth.sin(Turn) * Flat, -Mth.sin(Pitch), Mth.cos(Turn) * Flat);
    }

    public static Vector3f heading(Vector3f Normal, float XRot, float YRot) {
        Vector3f Way = flatten(look(XRot, YRot), Normal);
        if (Way.lengthSquared() < 1.0e-4f) {
            Way = flatten(look(0.0f, YRot), Normal);
        }
        if (Way.lengthSquared() < 1.0e-4f) {
            Way = flatten(new Vector3f(0.0f, 1.0f, 0.0f), Normal);
        }
        if (Way.lengthSquared() < 1.0e-4f) {
            Way = flatten(new Vector3f(0.0f, 0.0f, 1.0f), Normal);
        }
        return Way.normalize();
    }

    private static Vector3f flatten(Vector3f Way, Vector3f Normal) {
        return new Vector3f(Way).sub(new Vector3f(Normal).mul(Way.dot(Normal)));
    }

    public static Quaternionf orientation(Vector3f Normal, Vector3f Forward) {
        Vector3f Back = new Vector3f(Forward).negate();
        Vector3f Right = new Vector3f(Normal).cross(Back).normalize();
        return new Quaternionf().setFromNormalized(new Matrix3f(Right, Normal, Back));
    }

    public static Vector3f attachPoint(Direction Side, float Width, float Height) {
        return switch (Side) {
            case DOWN -> new Vector3f(0.0f, 0.0f, 0.0f);
            case UP -> new Vector3f(0.0f, Height, 0.0f);
            default -> new Vector3f(Side.getStepX() * Width * 0.5f, Height * 0.5f, Side.getStepZ() * Width * 0.5f);
        };
    }

    @Override
    protected void applyRotations(Scp538Spider Spider, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        Direction Side = Spider.surface();
        Vector3f Normal = new Vector3f(-Side.getStepX(), -Side.getStepY(), -Side.getStepZ());
        float YRot = Mth.rotLerp(PartialTick, Spider.yRotO, Spider.getYRot());
        float XRot = Mth.lerp(PartialTick, Spider.xRotO, Spider.getXRot());
        Quaternionf Wanted = orientation(Normal, heading(Normal, XRot, YRot));
        Vector3f Point = attachPoint(Side, Spider.getBbWidth(), Spider.getBbHeight());
        double Clock = Spider.tickCount + PartialTick;
        Quaternionf Current = Spider.turn();
        Vector3f Placed = Spider.attach();
        double Last = Spider.turnClock();
        if (Current == null || Placed == null || Double.isNaN(Last) || Clock < Last || Clock - Last > SnapGap) {
            Current = Wanted;
            Placed = Point;
        } else {
            float Share = 1.0f - (float) Math.pow(1.0f - TurnRate, Clock - Last);
            Current = new Quaternionf(Current).slerp(Wanted, Share);
            Placed = new Vector3f(Placed).lerp(Point, Share);
        }
        Spider.setTurn(Current, Placed, Clock);
        Pose.translate(Placed.x / Scale, Placed.y / Scale, Placed.z / Scale);
        Pose.mulPose(Current);
    }
}
