package net.rai.scp_unleashed.scp001scarlet.king.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.scp001scarlet.king.network.ScarletRiteFxPayload;

import java.util.ArrayList;
import java.util.List;

public final class ScarletRiteFx {
    public static final int Length = 320;
    public static final int RiseStart = 20;
    public static final int RiseEnd = 100;
    public static final int ShakeStart = 80;
    public static final int ShakeEnd = 150;
    public static final double PillarHeight = 64.0;
    private static final List<BlockPos> Seats = new ArrayList<>();
    private static BlockPos Throne = BlockPos.ZERO;
    private static long Start = Long.MIN_VALUE / 2;

    private ScarletRiteFx() {
    }

    public static void start(ScarletRiteFxPayload Payload) {
        Throne = Payload.Throne();
        Seats.clear();
        Seats.addAll(Payload.Seats());
        Start = Payload.Start();
    }

    private static float age(float Partial) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null) {
            return -1.0f;
        }
        float Age = Game.level.getGameTime() - Start + Partial;
        return Age < 0.0f || Age > Length ? -1.0f : Age;
    }

    private static float strength(float Age) {
        if (Age < 0.0f) {
            return 0.0f;
        }
        return Mth.clamp(Math.min(Age / 40.0f, (Length - Age) / 60.0f), 0.0f, 1.0f);
    }

    public static void fog(ViewportEvent.ComputeFogColor Event) {
        float Share = strength(age((float) Event.getPartialTick()));
        if (Share <= 0.0f) {
            return;
        }
        Event.setRed(Mth.lerp(Share * 0.8f, Event.getRed(), 0.22f));
        Event.setGreen(Mth.lerp(Share * 0.8f, Event.getGreen(), 0.02f));
        Event.setBlue(Mth.lerp(Share * 0.8f, Event.getBlue(), 0.03f));
    }

    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        float Age = age((float) Event.getPartialTick());
        if (Age < ShakeStart || Age > ShakeEnd) {
            return;
        }
        float Amount = 0.6f * Mth.sin((Age - ShakeStart) / (ShakeEnd - ShakeStart) * Mth.PI);
        Event.setRoll(Event.getRoll() + Mth.sin(Age * 2.1f) * Amount);
        Event.setPitch(Event.getPitch() + Mth.sin(Age * 1.7f + 1.0f) * Amount * 0.5f);
    }

    public static void render(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        float Age = age(Event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (Age < RiseStart || Seats.isEmpty()) {
            return;
        }
        Camera View = Event.getCamera();
        Vec3 Eye = View.getPosition();
        PoseStack Pose = Event.getPoseStack();
        MultiBufferSource.BufferSource Buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.lightning());
        float Rise = Mth.clamp((Age - RiseStart) / (RiseEnd - RiseStart), 0.0f, 1.0f);
        float Fade = strength(Age);
        Pose.pushPose();
        for (BlockPos Seat : Seats) {
            Vec3 Bottom = Vec3.atBottomCenterOf(Seat).subtract(Eye);
            Vec3 Top = Bottom.add(0.0, PillarHeight * Rise, 0.0);
            Vec3 Local = Vec3.ZERO;
            ScarletGlowDraw.ribbon(Consumer, Pose.last().pose(), Bottom, Top, Local, 3.2f, 1.0f, 0.12f, 0.08f, 0.25f * Fade);
            ScarletGlowDraw.ribbon(Consumer, Pose.last().pose(), Bottom, Top, Local, 1.0f, 1.0f, 0.45f, 0.3f, 0.6f * Fade);
        }
        Pose.popPose();
        Buffers.endBatch(RenderType.lightning());
    }
}
