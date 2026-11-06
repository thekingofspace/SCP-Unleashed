package net.rai.scp_unleashed.scp018.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp018.BallPath;
import net.rai.scp_unleashed.scp018.BallSegment;
import net.rai.scp_unleashed.scp018.BallTuning;
import net.rai.scp_unleashed.scp018.Scp018Ball;
import net.rai.scp_unleashed.scp018.Scp018Registry;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Scp018BallRenderer extends EntityRenderer<Scp018Ball> {
    public static final float ModelDiameter = 12.0f / 16.0f;
    public static final float BallScale = (float) (BallTuning.Radius * 2.0) / ModelDiameter;
    public static final float SpinRate = 0.55f;
    public static final float MaxSpinPerTick = 1.1f;
    private final ItemRenderer Items;
    private ItemStack Shown = ItemStack.EMPTY;

    public Scp018BallRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.Items = Context.getItemRenderer();
        this.shadowRadius = 0.1f;
        this.shadowStrength = 0.6f;
    }

    @Override
    public void render(Scp018Ball Ball, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (Shown.isEmpty()) {
            Shown = new ItemStack(Scp018Registry.BallItem.get());
        }
        Pose.pushPose();
        Pose.translate(0.0, BallTuning.Radius, 0.0);
        BallPath Path = Ball.path();
        BallClientData Data = BallEffects.data(Ball);
        if (!Ball.isRemoved() && !Path.isEmpty()) {
            double Time = BallClock.frame(Ball.level(), PartialTick);
            Vec3 Exact = Path.positionAt(Time);
            BallSegment Current = Path.at(Time);
            if (Exact != null && Current != null && Current.isFloating()) {
                double Phase = (Time - Current.StartTime()) / BallTuning.BobPeriod * Math.PI * 2.0;
                Exact = Exact.add(0.0, Math.sin(Phase) * BallTuning.BobAmplitude, 0.0);
            }
            if (Exact != null) {
                double X = Mth.lerp(PartialTick, Ball.xOld, Ball.getX());
                double Y = Mth.lerp(PartialTick, Ball.yOld, Ball.getY()) + BallTuning.Radius;
                double Z = Mth.lerp(PartialTick, Ball.zOld, Ball.getZ());
                Pose.translate(Exact.x - X, Exact.y - Y, Exact.z - Z);
            }
            spin(Data, Path, Time);
        }
        Pose.mulPose(Data.Spin);
        Pose.scale(BallScale, BallScale, BallScale);
        Items.renderStatic(Shown, ItemDisplayContext.NONE, PackedLight, OverlayTexture.NO_OVERLAY, Pose, Buffers, Ball.level(), Ball.getId());
        Pose.popPose();
        super.render(Ball, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    private static void spin(BallClientData Data, BallPath Path, double Time) {
        if (Double.isNaN(Data.SpinTime)) {
            Data.SpinTime = Time;
            return;
        }
        double Step = Mth.clamp(Time - Data.SpinTime, 0.0, 3.0);
        Data.SpinTime = Time;
        if (Step <= 0.0) {
            return;
        }
        Vec3 Velocity = Path.velocityAt(Time);
        double Speed = Velocity.length();
        if (Speed < 1.0e-4) {
            return;
        }
        Vector3f Axis = new Vector3f((float) Velocity.z, 0.0f, (float) -Velocity.x);
        if (Axis.lengthSquared() < 1.0e-8f) {
            Axis.set(1.0f, 0.0f, 0.0f);
        }
        Axis.normalize();
        float Angle = (float) Math.min(MaxSpinPerTick, Speed / BallTuning.Radius * SpinRate * 0.1) * (float) Step;
        Data.Spin.premul(new Quaternionf().rotationAxis(Angle, Axis.x, Axis.y, Axis.z)).normalize();
    }

    @Override
    public ResourceLocation getTextureLocation(Scp018Ball Ball) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
