package net.rai.scp_unleashed.scp3008.gear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carry;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008CarryParts;
import net.rai.scp_unleashed.scp3008.gear.network.Scp3008CarryActionPayload;
import net.rai.scp_unleashed.scp3008.gear.network.Scp3008CarrySyncPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp3008CarryClient {
    public static final float HeldSize = 0.6f;
    public static final float HeldCentre = 1.0f;
    public static final float HeldBack = 0.36f;
    public static final float CrouchDrop = 0.14f;
    public static final float ViewSize = 0.58f;
    public static final float ViewDown = -0.5f;
    public static final float ViewAhead = -0.62f;
    public static final float ViewTilt = 14.0f;
    public static final float TallFit = 1.4f;
    private static final Map<Integer, View> Views = new HashMap<>();
    private static boolean WasCarrying;
    private static boolean AwaitRelease;
    private static boolean Suppress;

    public record View(List<Scp3008CarrySyncPayload.Shown> Parts, Optional<Direction> Facing, int Turns) {
    }

    private Scp3008CarryClient() {
    }

    public static void sync(Scp3008CarrySyncPayload Payload) {
        if (Payload.Parts().isEmpty()) {
            Views.remove(Payload.EntityId());
            return;
        }
        Views.put(Payload.EntityId(), new View(List.copyOf(Payload.Parts()), Scp3008Carry.facingOfPayload(Payload.Facing()), Payload.Turns()));
    }

    public static boolean carrying(Entity Holder) {
        return Holder != null && Views.containsKey(Holder.getId());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Views.clear();
        WasCarrying = false;
        AwaitRelease = false;
        Suppress = false;
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent Event) {
        if (Event.getLevel().isClientSide() && Event.getEntity() instanceof Player) {
            Views.remove(Event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered Event) {
        LocalPlayer Self = Minecraft.getInstance().player;
        if (Self == null) {
            return;
        }
        if (carrying(Self)) {
            Event.setCanceled(true);
            Event.setSwingHand(false);
            if (Event.isUseItem() && !AwaitRelease) {
                AwaitRelease = true;
                PacketDistributor.sendToServer(new Scp3008CarryActionPayload(Scp3008CarryActionPayload.Use));
            }
            return;
        }
        if (Event.isUseItem() && Suppress) {
            Event.setCanceled(true);
            Event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre Event) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer Self = Game.player;
        if (Self == null) {
            WasCarrying = false;
            return;
        }
        boolean Now = carrying(Self);
        if (Now && !WasCarrying) {
            AwaitRelease = true;
        }
        if (!Now && WasCarrying) {
            Suppress = true;
        }
        WasCarrying = Now;
        if (!Game.options.keyUse.isDown()) {
            AwaitRelease = false;
            Suppress = false;
        }
        if (Now && Game.screen == null) {
            while (Game.options.keyDrop.consumeClick()) {
                PacketDistributor.sendToServer(new Scp3008CarryActionPayload(Scp3008CarryActionPayload.Drop));
            }
        }
    }

    @SubscribeEvent
    public static void hands(RenderHandEvent Event) {
        LocalPlayer Self = Minecraft.getInstance().player;
        View Seen = Self == null ? null : Views.get(Self.getId());
        if (Seen == null) {
            return;
        }
        Event.setCanceled(true);
        if (Event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        PoseStack Pose = Event.getPoseStack();
        Pose.pushPose();
        Pose.translate(0.0f, ViewDown, ViewAhead);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        Pose.mulPose(Axis.XP.rotationDegrees(-ViewTilt));
        renderPiece(Seen, Pose, Event.getMultiBufferSource(), Event.getPackedLight(), ViewSize);
        Pose.popPose();
    }

    @SubscribeEvent
    public static void posePlayer(RenderPlayerEvent.Pre Event) {
        if (!carrying(Event.getEntity())) {
            return;
        }
        PlayerModel<AbstractClientPlayer> Model = Event.getRenderer().getModel();
        Model.rightArmPose = HumanoidModel.ArmPose.BLOCK;
        Model.leftArmPose = HumanoidModel.ArmPose.BLOCK;
    }

    @SubscribeEvent
    public static void drawCarried(RenderPlayerEvent.Post Event) {
        Player Holder = Event.getEntity();
        View Seen = Views.get(Holder.getId());
        if (Seen == null) {
            return;
        }
        PoseStack Pose = Event.getPoseStack();
        float BodyYaw = Mth.rotLerp(Event.getPartialTick(), Holder.yBodyRotO, Holder.yBodyRot);
        float Scale = Holder.getScale();
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(-BodyYaw));
        Pose.scale(Scale, Scale, Scale);
        Pose.translate(0.0f, HeldCentre - (Holder.isCrouching() ? CrouchDrop : 0.0f), HeldBack);
        renderPiece(Seen, Pose, Event.getMultiBufferSource(), Event.getPackedLight(), HeldSize);
        Pose.popPose();
    }

    public static void renderPiece(View Seen, PoseStack Pose, MultiBufferSource Buffers, int Light, float Size) {
        Rotation Turn = Scp3008CarryParts.rotationFor(Seen.Facing(), Scp3008CarryParts.turned(Direction.SOUTH, Seen.Turns()));
        List<BlockPos> Offsets = new ArrayList<>();
        List<BlockState> States = new ArrayList<>();
        int MinX = Integer.MAX_VALUE;
        int MinY = Integer.MAX_VALUE;
        int MinZ = Integer.MAX_VALUE;
        int MaxX = Integer.MIN_VALUE;
        int MaxY = Integer.MIN_VALUE;
        int MaxZ = Integer.MIN_VALUE;
        for (Scp3008CarrySyncPayload.Shown Piece : Seen.Parts()) {
            BlockPos Offset = Scp3008CarryParts.rotated(Piece.Offset(), Turn);
            Offsets.add(Offset);
            States.add(Scp3008CarryParts.rotated(Piece.State(), Turn));
            MinX = Math.min(MinX, Offset.getX());
            MinY = Math.min(MinY, Offset.getY());
            MinZ = Math.min(MinZ, Offset.getZ());
            MaxX = Math.max(MaxX, Offset.getX() + 1);
            MaxY = Math.max(MaxY, Offset.getY() + 1);
            MaxZ = Math.max(MaxZ, Offset.getZ() + 1);
        }
        int Extent = Math.max(MaxX - MinX, Math.max(MaxY - MinY, MaxZ - MinZ));
        float Fit = Size * Math.min(1.0f, TallFit / Extent);
        BlockRenderDispatcher Blocks = Minecraft.getInstance().getBlockRenderer();
        Pose.pushPose();
        Pose.scale(Fit, Fit, Fit);
        Pose.translate(-(MinX + MaxX) / 2.0f, -(MinY + MaxY) / 2.0f, -MinZ);
        for (int Index = 0; Index < Offsets.size(); Index++) {
            BlockPos Offset = Offsets.get(Index);
            Pose.pushPose();
            Pose.translate(Offset.getX(), Offset.getY(), Offset.getZ());
            Blocks.renderSingleBlock(States.get(Index), Pose, Buffers, Light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            Pose.popPose();
        }
        Pose.popPose();
    }
}
