package net.rai.scp_unleashed.z0003.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Z0003FieldPass {
    public static final int MaxPending = 64;

    private static final List<Queued> Pending = new ArrayList<>();

    private Z0003FieldPass() {
    }

    public record Queued(Vec3 Centre, Z0003FieldLayers.Look Shown) {
    }

    public static void reset() {
        Pending.clear();
    }

    public static void queue(Vec3 Centre, Z0003FieldLayers.Look Shown) {
        if (Pending.size() >= MaxPending) {
            Pending.clear();
        }
        Pending.add(new Queued(Centre, Shown));
    }

    @SubscribeEvent
    public static void stage(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || Pending.isEmpty()) {
            return;
        }
        try {
            draw(Event);
        } finally {
            Pending.clear();
        }
    }

    private static void draw(RenderLevelStageEvent Event) {
        Vec3 Eye = Event.getCamera().getPosition();
        Quaternionf Turn = Event.getCamera().rotation();
        Pending.sort(Comparator.comparingDouble((Queued Each) -> Each.Centre().distanceToSqr(Eye)).reversed());
        Matrix4fStack Stack = RenderSystem.getModelViewStack();
        Stack.pushMatrix();
        Stack.mul(Event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        MultiBufferSource.BufferSource Buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack Pose = new PoseStack();
        try {
            for (Queued Each : Pending) {
                Pose.pushPose();
                Pose.translate(Each.Centre().x - Eye.x, Each.Centre().y - Eye.y, Each.Centre().z - Eye.z);
                Z0003FieldLayers.render(Pose, Buffers, Each.Shown(), Eye.subtract(Each.Centre()), Turn);
                Pose.popPose();
                Buffers.endBatch();
            }
        } finally {
            Buffers.endBatch();
            Stack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }
}
