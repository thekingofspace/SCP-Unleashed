package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.item.WireCutterItem;
import net.rai.scp_unleashed.network.WireCutterPayload;
import net.rai.scp_unleashed.wire.WireCutting;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class WireCutterClient {
    private static final double OutlineGrow = 0.6 / 16.0;

    @Nullable
    private static InteractionHand cutterHand(LocalPlayer User) {
        for (InteractionHand Hand : InteractionHand.values()) {
            if (User.getItemInHand(Hand).getItem() instanceof WireCutterItem) {
                return Hand;
            }
        }
        return null;
    }

    @Nullable
    private static WireCutting.WireHit target(float Partial) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        ClientLevel World = Game.level;
        if (User == null || World == null || User.isSpectator() || cutterHand(User) == null) {
            return null;
        }
        Vec3 Eye = User.getEyePosition(Partial);
        WireCutting.WireHit Hit = WireCutting.pick(World, Eye, User.getViewVector(Partial), User.blockInteractionRange());
        if (Hit == null) {
            return null;
        }
        HitResult Aimed = Game.hitResult;
        if (Aimed != null && Aimed.getType() != HitResult.Type.MISS && Aimed.getLocation().distanceTo(Eye) + 1.0E-3 < Hit.Distance()) {
            return null;
        }
        return Hit;
    }

    @SubscribeEvent
    public static void useKey(InputEvent.InteractionKeyMappingTriggered Event) {
        LocalPlayer User = Minecraft.getInstance().player;
        if (!Event.isUseItem() || User == null || !(User.getItemInHand(Event.getHand()).getItem() instanceof WireCutterItem)) {
            return;
        }
        WireCutting.WireHit Hit = target(1.0f);
        if (Hit == null) {
            return;
        }
        PacketDistributor.sendToServer(new WireCutterPayload(Hit.Owner(), Hit.Target(), Event.getHand() == InteractionHand.MAIN_HAND));
        Event.setSwingHand(true);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void blockHighlight(RenderHighlightEvent.Block Event) {
        if (target(Event.getDeltaTracker().getGameTimeDeltaPartialTick(true)) != null) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        WireCutting.WireHit Hit = target(Event.getPartialTick().getGameTimeDeltaPartialTick(true));
        if (Hit == null) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        Vec3 Camera = Event.getCamera().getPosition();
        PoseStack Pose = Event.getPoseStack();
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        Pose.pushPose();
        Pose.translate(-Camera.x, -Camera.y, -Camera.z);
        outline(Pose, Buffers.getBuffer(RenderType.lines()), Hit.From(), Hit.To(), Hit.HalfWidth() + OutlineGrow);
        Pose.popPose();
        Buffers.endBatch(RenderType.lines());
    }

    private static void outline(PoseStack Pose, VertexConsumer Consumer, Vec3 From, Vec3 To, double HalfWidth) {
        Vec3 Span = To.subtract(From);
        double Length = Span.length();
        if (Length < 1.0E-4) {
            return;
        }
        Vec3 Forward = Span.scale(1.0 / Length);
        Vec3 Up = Math.abs(Forward.y) > 0.99 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 Side = Forward.cross(Up).normalize().scale(HalfWidth);
        Vec3 Lift = Side.cross(Forward).normalize().scale(HalfWidth);
        Vec3[] Corners = {Side.add(Lift), Side.subtract(Lift), Side.scale(-1.0).subtract(Lift), Side.scale(-1.0).add(Lift)};
        PoseStack.Pose Last = Pose.last();
        for (int Index = 0; Index < 4; Index++) {
            Vec3 Corner = Corners[Index];
            Vec3 Next = Corners[(Index + 1) % 4];
            line(Consumer, Last, From.add(Corner), To.add(Corner));
            line(Consumer, Last, From.add(Corner), From.add(Next));
            line(Consumer, Last, To.add(Corner), To.add(Next));
        }
    }

    private static void line(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Start, Vec3 End) {
        Vec3 Normal = End.subtract(Start).normalize();
        Consumer.addVertex(Last, (float) Start.x, (float) Start.y, (float) Start.z).setColor(0.0f, 0.0f, 0.0f, 0.4f).setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
        Consumer.addVertex(Last, (float) End.x, (float) End.y, (float) End.z).setColor(0.0f, 0.0f, 0.0f, 0.4f).setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }
}
