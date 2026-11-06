package net.rai.scp_unleashed.extraction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ExtractionBeamRenderer {
    public static final Vector3f EmitterTip = new Vector3f(0.5F, 0.53125F, 0.03125F);
    private static final float HandFovScale = (float) (1.0 / Math.tan(Math.toRadians(35.0)));

    private ExtractionBeamRenderer() {
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ExtractionTracker.sessions().isEmpty()) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera View = Event.getCamera();
        Vec3 Eye = View.getPosition();
        float Time = (World.getGameTime() % 72000L) + Partial;
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        VertexConsumer Consumer = Buffers.getBuffer(ExtractionRenderTypes.Beam);
        PoseStack.Pose Last = Event.getPoseStack().last();
        for (ExtractionTracker.Session Current : ExtractionTracker.sessions()) {
            Entity Owner = World.getEntity(Current.getUserId());
            Entity Target = World.getEntity(Current.getTargetId());
            if (!Current.isLocked() || !(Owner instanceof Player User) || Target == null || Target.isRemoved()) {
                continue;
            }
            Vec3 Start = emitterPosition(Game, User, View, Event.getProjectionMatrix(), Partial);
            Vec3 End = Target.getPosition(Partial).add(0.0, Target.getBbHeight() * 0.5, 0.0);
            ExtractionBeam.draw(Consumer, Last, Start.subtract(Eye), End.subtract(Eye), Current.getColour(), Current.getProgress(Partial), Time, Current.getUserId());
        }
        Buffers.endBatch(ExtractionRenderTypes.Beam);
    }

    public static Vec3 emitterPosition(Minecraft Game, Player User, Camera View, Matrix4f Projection, float Partial) {
        ItemStack Stack = User.getUseItem();
        boolean LeftHand = ExtractionTracker.armOf(User, User.getUsedItemHand()) == HumanoidArm.LEFT;
        BakedModel Model = Game.getItemRenderer().getModel(Stack, User.level(), User, User.getId());
        if (User == Game.player && Game.options.getCameraType().isFirstPerson() && User instanceof LocalPlayer Local) {
            return firstPersonEmitter(Local, Model, LeftHand, View, Projection, Partial);
        }
        return thirdPersonEmitter(User, Model, LeftHand, View.getPosition(), Partial);
    }

    private static Vec3 firstPersonEmitter(LocalPlayer Local, BakedModel Model, boolean LeftHand, Camera View, Matrix4f Projection, float Partial) {
        PoseStack Hand = new PoseStack();
        Hand.mulPose(Axis.XP.rotationDegrees((Local.getViewXRot(Partial) - Mth.lerp(Partial, Local.xBobO, Local.xBob)) * 0.1F));
        Hand.mulPose(Axis.YP.rotationDegrees((Local.getViewYRot(Partial) - Mth.lerp(Partial, Local.yBobO, Local.yBob)) * 0.1F));
        RiftExtractorClientExtensions.applyHeldPose(Hand, LeftHand ? HumanoidArm.LEFT : HumanoidArm.RIGHT, 0.0F, ExtractionTracker.raise(Partial),
                ExtractionTracker.tremor(Partial, 0.0F), ExtractionTracker.tremor(Partial, 1.7F));
        Model.applyTransform(LeftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, Hand, LeftHand);
        Hand.translate(-0.5F, -0.5F, -0.5F);
        Vector3f Tip = Hand.last().pose().transformPosition(EmitterTip, new Vector3f());
        float Spread = HandFovScale / Math.max(0.05F, Projection.m11());
        Vector3f Look = View.getLookVector();
        Vector3f Up = View.getUpVector();
        Vector3f Left = View.getLeftVector();
        double Across = -Tip.x() * Spread;
        double Rise = Tip.y() * Spread;
        double Depth = -Tip.z();
        return View.getPosition().add(
                Look.x() * Depth + Up.x() * Rise - Left.x() * Across,
                Look.y() * Depth + Up.y() * Rise - Left.y() * Across,
                Look.z() * Depth + Up.z() * Rise - Left.z() * Across);
    }

    private static Vec3 thirdPersonEmitter(Player User, BakedModel Model, boolean LeftHand, Vec3 Eye, float Partial) {
        float BodyYaw = Mth.rotLerp(Partial, User.yBodyRotO, User.yBodyRot);
        float HeadYaw = Mth.wrapDegrees(Mth.rotLerp(Partial, User.yHeadRotO, User.yHeadRot) - BodyYaw);
        float HeadPitch = User.getViewXRot(Partial);
        boolean Crouching = User.isCrouching();
        boolean Slim = User instanceof AbstractClientPlayer Client && Client.getSkin().model() == PlayerSkin.Model.SLIM;
        float Age = User.tickCount + Partial;
        float Side = LeftHand ? -1.0F : 1.0F;
        float PivotX = -Side * (Slim ? 4.5F : 5.0F);
        float PivotY = Crouching ? 5.2F : 2.0F;
        float ArmX = -Mth.HALF_PI + HeadPitch * Mth.DEG_TO_RAD + RiftExtractorClientExtensions.AimPitch + (Crouching ? 0.4F : 0.0F) + Side * Mth.sin(Age * 0.067F) * 0.05F;
        float ArmY = -Side * RiftExtractorClientExtensions.AimYaw + HeadYaw * Mth.DEG_TO_RAD;
        float ArmZ = Side * (Mth.cos(Age * 0.09F) * 0.05F + 0.05F);
        float Scale = User.getScale();
        Vec3 Position = User.getPosition(Partial).subtract(Eye);
        PoseStack Pose = new PoseStack();
        Pose.translate(Position.x, Position.y + (Crouching ? Scale * -2.0F / 16.0F : 0.0F), Position.z);
        Pose.scale(Scale, Scale, Scale);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0F - BodyYaw));
        Pose.scale(-1.0F, -1.0F, 1.0F);
        Pose.scale(0.9375F, 0.9375F, 0.9375F);
        Pose.translate(0.0F, -1.501F, 0.0F);
        Pose.translate(PivotX / 16.0F, PivotY / 16.0F, 0.0F);
        Pose.mulPose(new Quaternionf().rotationZYX(ArmZ, ArmY, ArmX));
        Pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        Pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        Pose.translate(Side / 16.0F, 0.125F, -0.625F);
        Model.applyTransform(LeftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, Pose, LeftHand);
        Pose.translate(-0.5F, -0.5F, -0.5F);
        Vector3f Tip = Pose.last().pose().transformPosition(EmitterTip, new Vector3f());
        return Eye.add(Tip.x(), Tip.y(), Tip.z());
    }
}
