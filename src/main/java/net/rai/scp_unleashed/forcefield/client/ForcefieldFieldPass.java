package net.rai.scp_unleashed.forcefield.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.forcefield.ForcefieldEmitterBlockEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ForcefieldFieldPass {
    public static final int MaxPending = 512;
    public static final int BufferBytes = 262144;

    private static final List<Queued> Pending = new ArrayList<>();
    private static final Set<ForcefieldEmitterBlockEntity> Listed = Collections.newSetFromMap(new IdentityHashMap<>());
    private static ForcefieldDrawPlan.Pass PendingPass = ForcefieldDrawPlan.Pass.Direct;
    private static MultiBufferSource.BufferSource Buffers;

    private ForcefieldFieldPass() {
    }

    public record Queued(ForcefieldEmitterBlockEntity Emitter, float PartialTick) {
    }

    public static ForcefieldDrawPlan.Pass currentPass() {
        return ForcefieldDrawPlan.pass(ShaderCompat.useFallback(), fabulous());
    }

    public static boolean fabulous() {
        LevelRenderer Renderer = Minecraft.getInstance().levelRenderer;
        return Renderer != null && Renderer.getParticlesTarget() != null;
    }

    @Nullable
    public static RenderLevelStageEvent.Stage stageFor(ForcefieldDrawPlan.Pass Chosen) {
        return switch (Chosen) {
            case AfterBlockEntities -> RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
            case AfterParticles -> RenderLevelStageEvent.Stage.AFTER_PARTICLES;
            case Direct -> null;
        };
    }

    public static void queue(ForcefieldEmitterBlockEntity Emitter, float PartialTick, ForcefieldDrawPlan.Pass Chosen) {
        if (Pending.size() >= MaxPending || !Listed.add(Emitter)) {
            return;
        }
        PendingPass = Chosen;
        Pending.add(new Queued(Emitter, PartialTick));
    }

    public static void reset() {
        Pending.clear();
        Listed.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void stage(RenderLevelStageEvent Event) {
        RenderLevelStageEvent.Stage Current = Event.getStage();
        if (Current == RenderLevelStageEvent.Stage.AFTER_SKY || Current == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            reset();
            return;
        }
        if (Pending.isEmpty() || Current != stageFor(PendingPass)) {
            return;
        }
        try {
            draw(Event, PendingPass);
        } finally {
            reset();
        }
    }

    private static void draw(RenderLevelStageEvent Event, ForcefieldDrawPlan.Pass Chosen) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null) {
            return;
        }
        if (ForcefieldDrawPlan.flushesSceneFirst(Chosen)) {
            Client.renderBuffers().bufferSource().endBatch();
        }
        if (ForcefieldDrawPlan.bindsParticleTarget(Chosen)) {
            RenderTarget Target = Client.levelRenderer.getParticlesTarget();
            if (Target != null) {
                Target.bindWrite(false);
            }
        }
        if (World.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel();
        } else {
            Lighting.setupLevel();
        }
        Vec3 Eye = Event.getCamera().getPosition();
        Pending.sort(Comparator.comparing((Queued Each) -> distanceSqr(Each.Emitter(), Eye), ForcefieldDrawPlan.FarFirst));
        PoseStack Pose = Event.getPoseStack() != null ? Event.getPoseStack() : new PoseStack();
        MultiBufferSource.BufferSource Own = buffers();
        for (Queued Each : Pending) {
            Pose.pushPose();
            place(Pose, Each.Emitter(), Eye);
            ForcefieldRenderer.draw(Each.Emitter(), Each.PartialTick(), Pose.last(), Own);
            Pose.popPose();
        }
        Own.endBatch();
        if (ForcefieldDrawPlan.stampsDepth(Chosen)) {
            for (Queued Each : Pending) {
                Pose.pushPose();
                place(Pose, Each.Emitter(), Eye);
                ForcefieldRenderer.stamp(Each.Emitter(), Each.PartialTick(), Pose.last(), Own);
                Pose.popPose();
            }
            Own.endBatch();
        }
    }

    private static void place(PoseStack Pose, ForcefieldEmitterBlockEntity Emitter, Vec3 Eye) {
        BlockPos Pos = Emitter.getBlockPos();
        Pose.translate(Pos.getX() - Eye.x, Pos.getY() - Eye.y, Pos.getZ() - Eye.z);
    }

    private static double distanceSqr(ForcefieldEmitterBlockEntity Emitter, Vec3 Eye) {
        BlockPos Pos = Emitter.getBlockPos();
        return Eye.distanceToSqr(Pos.getX() + 0.5, Pos.getY() + 1.0 + Emitter.getHeight() / 2.0, Pos.getZ() + 0.5);
    }

    private static MultiBufferSource.BufferSource buffers() {
        if (Buffers == null) {
            Buffers = MultiBufferSource.immediate(new ByteBufferBuilder(BufferBytes));
        }
        return Buffers;
    }
}
