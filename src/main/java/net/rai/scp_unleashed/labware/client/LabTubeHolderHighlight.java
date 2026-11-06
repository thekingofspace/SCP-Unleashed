package net.rai.scp_unleashed.labware.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabTubeHolderBlock;
import net.rai.scp_unleashed.labware.LabTubeHolderBlockEntity;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class LabTubeHolderHighlight {
    public static final float EmptyRed = 0.62f;
    public static final float EmptyGreen = 0.86f;
    public static final float EmptyBlue = 0.96f;
    public static final float FilledRed = 1.0f;
    public static final float FilledGreen = 0.82f;
    public static final float FilledBlue = 0.34f;
    public static final float Alpha = 0.85f;

    private LabTubeHolderHighlight() {
    }

    @SubscribeEvent
    public static void renderSlot(RenderHighlightEvent.Block Event) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null) {
            return;
        }
        BlockHitResult Target = Event.getTarget();
        BlockPos Pos = Target.getBlockPos();
        BlockState State = Game.level.getBlockState(Pos);
        if (!(State.getBlock() instanceof LabTubeHolderBlock) || !(Game.level.getBlockEntity(Pos) instanceof LabTubeHolderBlockEntity Holder)) {
            return;
        }
        int Slot = LabTubeHolderBlock.slotFromHit(State, Pos, Target.getLocation());
        AABB Box = LabTubeHolderBlock.slotBox(State, Pos, Slot);
        Vec3 Camera = Event.getCamera().getPosition();
        PoseStack Pose = Event.getPoseStack();
        VertexConsumer Lines = Event.getMultiBufferSource().getBuffer(RenderType.lines());
        boolean Filled = !Holder.getTube(Slot).isEmpty();
        LevelRenderer.renderLineBox(Pose, Lines, Box.move(-Camera.x, -Camera.y, -Camera.z),
                Filled ? FilledRed : EmptyRed, Filled ? FilledGreen : EmptyGreen, Filled ? FilledBlue : EmptyBlue, Alpha);
    }
}
