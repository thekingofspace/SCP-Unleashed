package net.rai.scp_unleashed.labware.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.labware.LabTubeHolderBlock;
import net.rai.scp_unleashed.labware.LabTubeHolderBlockEntity;

public class LabTubeHolderRenderer implements BlockEntityRenderer<LabTubeHolderBlockEntity> {
    public static final float Scale = 0.58f;
    public static final float BaseY = 0.3475f;

    private final Minecraft Game;

    public LabTubeHolderRenderer(BlockEntityRendererProvider.Context Context) {
        this.Game = Minecraft.getInstance();
    }

    @Override
    public void render(LabTubeHolderBlockEntity Holder, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Holder.getLevel();
        if (World == null) {
            return;
        }
        Direction Front = Holder.getFacing();
        for (int Slot = 0; Slot < LabTubeHolderBlockEntity.SlotCount; Slot++) {
            ItemStack Tube = Holder.getTube(Slot);
            if (Tube.isEmpty()) {
                continue;
            }
            Vec3 Centre = LabTubeHolderBlock.slotCentre(Front, Slot);
            Pose.pushPose();
            Pose.translate(Centre.x, BaseY, Centre.z);
            Pose.scale(Scale, Scale, Scale);
            Game.getItemRenderer().renderStatic(Tube, ItemDisplayContext.NONE, PackedLight, PackedOverlay, Pose, Buffers, World, (int) Holder.getBlockPos().asLong() + Slot);
            Pose.popPose();
        }
    }
}
