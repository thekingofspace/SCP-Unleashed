package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.LargeBlock;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class LargeBlockPreview {
    private static final double OutlineGap = 0.003;
    private static final double CellGap = 0.008;

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer Player = Game.player;
        if (Player == null || Game.level == null || Player.isSpectator() || !(Game.hitResult instanceof BlockHitResult Hit) || Hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Stack = Player.getItemInHand(Hand);
            if (Stack.getItem() instanceof BlockItem Item) {
                if (Item.getBlock() instanceof LargeBlock Large) {
                    BlockPlaceContext Context = Item.updatePlacementContext(new BlockPlaceContext(Player, Hand, Stack, Hit));
                    if (Context != null) {
                        List<BlockPos> Footprint = Large.getFootprint(Context);
                        draw(Event, Game, Footprint, LargeBlock.blockedCells(Context, Footprint));
                    }
                }
                return;
            }
        }
    }

    private static void draw(RenderLevelStageEvent Event, Minecraft Game, List<BlockPos> Footprint, List<BlockPos> Blocked) {
        if (Footprint.isEmpty()) {
            return;
        }
        Vec3 Camera = Event.getCamera().getPosition();
        PoseStack Pose = Event.getPoseStack();
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        VertexConsumer Lines = Buffers.getBuffer(RenderType.lines());
        Pose.pushPose();
        Pose.translate(-Camera.x, -Camera.y, -Camera.z);
        AABB Whole = LargeBlock.bounds(Footprint).inflate(OutlineGap);
        if (Blocked.isEmpty()) {
            LevelRenderer.renderLineBox(Pose, Lines, Whole, 1.0f, 1.0f, 1.0f, 0.85f);
        } else {
            LevelRenderer.renderLineBox(Pose, Lines, Whole, 1.0f, 0.3f, 0.3f, 0.9f);
            for (BlockPos Cell : Blocked) {
                LevelRenderer.renderLineBox(Pose, Lines, new AABB(Cell).inflate(CellGap), 1.0f, 0.15f, 0.15f, 1.0f);
            }
        }
        Pose.popPose();
        Buffers.endBatch(RenderType.lines());
    }
}
