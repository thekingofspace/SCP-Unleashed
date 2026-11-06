package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.item.WireCoilItem;
import net.rai.scp_unleashed.registry.ModDataComponents;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WirePath;
import net.rai.scp_unleashed.wire.WireTier;
import net.rai.scp_unleashed.wire.Wires;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class WirePreview {
    private static final int BlockedColour = 0xFFFF5A5A;

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer Player = Game.player;
        ClientLevel World = Game.level;
        if (Player == null || World == null) {
            return;
        }
        ItemStack Held = heldCoil(Player);
        if (Held == null || !(Held.getItem() instanceof WireCoilItem Coil)) {
            return;
        }
        GlobalPos Start = Held.get(ModDataComponents.WireStart.get());
        if (Start == null || !Start.dimension().equals(World.dimension()) || !World.isLoaded(Start.pos())
                || !(World.getBlockEntity(Start.pos()) instanceof OutletBlockEntity Outlet)) {
            return;
        }
        WireTier Tier = Coil.getTier();
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 Eye = Player.getEyePosition(Partial);
        Vec3 Look = Player.getViewVector(Partial);
        Vec3 Right = Look.cross(new Vec3(0.0, 1.0, 0.0));
        Right = Right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : Right.normalize();
        Vec3 To = Eye.add(Look.scale(0.6)).add(Right.scale(0.3)).add(0.0, -0.35, 0.0);
        Direction Face = Wires.settledFace(Outlet, Held.getOrDefault(ModDataComponents.WireStartFace.get(), Outlet.getFacing()), To);
        Vec3 From = Outlet.attachAt(Face);
        boolean Blocked = From.distanceTo(To) > Tier.getMaxLength() || WirePath.firstObstruction(World, From, To, Start.pos(), BlockPos.containing(To)) != null;
        int Colour = Blocked ? BlockedColour : 0xFFFFFFFF;
        Vec3 Camera = Event.getCamera().getPosition();
        PoseStack Pose = Event.getPoseStack();
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        Pose.pushPose();
        Pose.translate(-Camera.x, -Camera.y, -Camera.z);
        WireRenderer.render(Pose, Buffers, From, To, WireRenderer.anchorLight(World, Start.pos(), Face, Outlet.isFullBlock()), LevelRenderer.getLightColor(World, BlockPos.containing(To)), Colour, Tier,
                Wires.halfWidth(Tier, WireLink.freeSlot(Outlet.getLinks(), List.of()), Start.pos(), Start.pos()));
        Pose.popPose();
        Buffers.endBatch(WireRenderer.typeFor(Tier));
    }

    @Nullable
    private static ItemStack heldCoil(LocalPlayer Player) {
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Stack = Player.getItemInHand(Hand);
            if (Stack.has(ModDataComponents.WireStart.get())) {
                return Stack;
            }
        }
        return null;
    }
}
