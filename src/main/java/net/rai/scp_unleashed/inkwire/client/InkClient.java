package net.rai.scp_unleashed.inkwire.client;

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
import net.rai.scp_unleashed.inkwire.InkMarks;
import net.rai.scp_unleashed.inkwire.InkWirePayload;
import net.rai.scp_unleashed.inkwire.InvisibleInkItem;
import net.rai.scp_unleashed.wire.WireCutting;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class InkClient {
    private static final double OutlineGrow = 0.6 / 16.0;
    private static final float OutlineRed = 0.55f;
    private static final float OutlineGreen = 0.82f;
    private static final float OutlineBlue = 1.0f;
    private static final float OutlineAlpha = 0.7f;

    private InkClient() {
    }

    @Nullable
    private static WireCutting.WireHit target(float Partial) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        ClientLevel World = Game.level;
        if (User == null || World == null || User.isSpectator() || !InvisibleInkItem.isHeldBy(User)) {
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
        if (!Event.isUseItem() || User == null || !(User.getItemInHand(Event.getHand()).getItem() instanceof InvisibleInkItem)) {
            return;
        }
        WireCutting.WireHit Hit = target(1.0f);
        if (Hit == null) {
            return;
        }
        PacketDistributor.sendToServer(new InkWirePayload(Hit.Owner(), Hit.Target(), Event.getHand() == InteractionHand.MAIN_HAND));
        Event.setSwingHand(true);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void blockHighlight(RenderHighlightEvent.Block Event) {
        if (InkGhosts.revealing()) {
            if (target(Event.getDeltaTracker().getGameTimeDeltaPartialTick(true)) != null) {
                Event.setCanceled(true);
            }
            return;
        }
        ClientLevel World = Minecraft.getInstance().level;
        if (World != null && InkMarks.isHidden(World.getBlockState(Event.getTarget().getBlockPos()))) {
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
        Vec3 Viewpoint = Event.getCamera().getPosition();
        PoseStack Poses = Event.getPoseStack();
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        Poses.pushPose();
        Poses.translate(-Viewpoint.x, -Viewpoint.y, -Viewpoint.z);
        outline(Poses, Buffers.getBuffer(RenderType.lines()), Hit.From(), Hit.To(), Hit.HalfWidth() + OutlineGrow);
        Poses.popPose();
        Buffers.endBatch(RenderType.lines());
    }

    private static void outline(PoseStack Poses, VertexConsumer Sink, Vec3 From, Vec3 To, double HalfWidth) {
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
        PoseStack.Pose Last = Poses.last();
        for (int Index = 0; Index < 4; Index++) {
            Vec3 Corner = Corners[Index];
            Vec3 Next = Corners[(Index + 1) % 4];
            line(Sink, Last, From.add(Corner), To.add(Corner));
            line(Sink, Last, From.add(Corner), From.add(Next));
            line(Sink, Last, To.add(Corner), To.add(Next));
        }
    }

    private static void line(VertexConsumer Sink, PoseStack.Pose Last, Vec3 Start, Vec3 End) {
        Vec3 Normal = End.subtract(Start).normalize();
        Sink.addVertex(Last, (float) Start.x, (float) Start.y, (float) Start.z).setColor(OutlineRed, OutlineGreen, OutlineBlue, OutlineAlpha)
                .setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
        Sink.addVertex(Last, (float) End.x, (float) End.y, (float) End.z).setColor(OutlineRed, OutlineGreen, OutlineBlue, OutlineAlpha)
                .setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }
}
