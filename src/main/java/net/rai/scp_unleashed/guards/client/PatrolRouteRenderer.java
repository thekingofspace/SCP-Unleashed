package net.rai.scp_unleashed.guards.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guards.GuardsRegistry;
import net.rai.scp_unleashed.guards.PatrolLinkerItem;
import net.rai.scp_unleashed.guards.PatrolManagerBlockEntity;
import net.rai.scp_unleashed.guards.PatrolPoint;
import net.rai.scp_unleashed.guards.PatrolRoute;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class PatrolRouteRenderer {
    public static final double MaxDistance = 96.0;
    public static final float LineRed = 0.96f;
    public static final float LineGreen = 0.74f;
    public static final float LineBlue = 0.22f;
    public static final float RunnerLength = 0.35f;
    public static final int RunnerTicks = 24;
    public static final int LabelColour = 0xFFF2C14E;
    public static final int LabelBackground = 0x80101216;

    private PatrolRouteRenderer() {
    }

    @Nullable
    private static ItemStack carried(LocalPlayer User) {
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Stack = User.getItemInHand(Hand);
            if (Stack.has(GuardsRegistry.Route.get()) && (Stack.is(GuardsRegistry.PatrolLinker.get()) || Stack.is(GuardsRegistry.PatrolOrders.get()))) {
                return Stack;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null || Game.level == null) {
            return;
        }
        ItemStack Stack = carried(User);
        if (Stack == null) {
            return;
        }
        PatrolRoute Route = Stack.get(GuardsRegistry.Route.get());
        if (Route == null || !Game.level.dimension().equals(Route.Dimension())) {
            return;
        }
        List<PatrolPoint> Points = Route.Points();
        boolean PingPong = Route.PingPong();
        if (Stack.getItem() instanceof PatrolLinkerItem && Game.level.isLoaded(Route.Manager())
                && Game.level.getBlockEntity(Route.Manager()) instanceof PatrolManagerBlockEntity Live && Live.getRouteId().equals(Route.RouteId())) {
            Points = Live.getPoints();
            PingPong = Live.isPingPong();
        }
        Vec3 Camera = Event.getCamera().getPosition();
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float Phase = ((Game.level.getGameTime() % RunnerTicks) + Partial) / RunnerTicks;
        PoseStack Pose = Event.getPoseStack();
        Pose.pushPose();
        Pose.translate(-Camera.x, -Camera.y, -Camera.z);
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        VertexConsumer Lines = Buffers.getBuffer(RenderType.lines());
        BlockPos Manager = Route.Manager();
        if (!Points.isEmpty()) {
            line(Pose, Lines, Vec3.atCenterOf(Manager), anchor(Points.get(0).Pos()), 0.55f, 0.6f, 0.65f, 0.45f);
        }
        LevelRenderer.renderLineBox(Pose, Lines, new AABB(Manager).inflate(0.02), 0.55f, 0.6f, 0.65f, 0.6f);
        for (int Index = 0; Index < Points.size(); Index++) {
            BlockPos Pos = Points.get(Index).Pos();
            if (Vec3.atCenterOf(Pos).distanceToSqr(Camera) > MaxDistance * MaxDistance) {
                continue;
            }
            LevelRenderer.renderLineBox(Pose, Lines, new AABB(Pos.getX() + 0.1, Pos.getY(), Pos.getZ() + 0.1, Pos.getX() + 0.9, Pos.getY() + 0.25, Pos.getZ() + 0.9),
                    LineRed, LineGreen, LineBlue, 0.9f);
        }
        int Segments = Points.size() < 2 ? 0 : PingPong || Points.size() == 2 ? Points.size() - 1 : Points.size();
        for (int Index = 0; Index < Segments; Index++) {
            Vec3 Start = anchor(Points.get(Index).Pos());
            Vec3 End = anchor(Points.get((Index + 1) % Points.size()).Pos());
            line(Pose, Lines, Start, End, LineRed, LineGreen, LineBlue, 0.8f);
            Vec3 Span = End.subtract(Start);
            double Length = Span.length();
            if (Length > 0.5) {
                Vec3 Head = Start.add(Span.scale(Phase));
                Vec3 Tail = Start.add(Span.scale(Math.max(0.0, Phase - RunnerLength / Length)));
                line(Pose, Lines, Tail.add(0.0, 0.03, 0.0), Head.add(0.0, 0.03, 0.0), 1.0f, 0.95f, 0.75f, 1.0f);
            }
        }
        Buffers.endBatch(RenderType.lines());
        Font Text = Game.font;
        for (int Index = 0; Index < Points.size(); Index++) {
            BlockPos Pos = Points.get(Index).Pos();
            if (Vec3.atCenterOf(Pos).distanceToSqr(Camera) > MaxDistance * MaxDistance) {
                continue;
            }
            String Label = String.valueOf(Index + 1);
            Pose.pushPose();
            Pose.translate(Pos.getX() + 0.5, Pos.getY() + 0.9, Pos.getZ() + 0.5);
            Pose.mulPose(Game.getEntityRenderDispatcher().cameraOrientation());
            Pose.scale(0.03f, -0.03f, 0.03f);
            Text.drawInBatch(Label, -Text.width(Label) / 2.0f, 0.0f, LabelColour, false, Pose.last().pose(), Buffers, Font.DisplayMode.SEE_THROUGH, LabelBackground,
                    LightTexture.FULL_BRIGHT);
            Pose.popPose();
        }
        Buffers.endBatch();
        Pose.popPose();
    }

    private static Vec3 anchor(BlockPos Pos) {
        return new Vec3(Pos.getX() + 0.5, Pos.getY() + 0.2, Pos.getZ() + 0.5);
    }

    private static void line(PoseStack Pose, VertexConsumer Lines, Vec3 Start, Vec3 End, float Red, float Green, float Blue, float Alpha) {
        Vec3 Normal = End.subtract(Start);
        if (Normal.lengthSqr() < 1.0E-6) {
            return;
        }
        Normal = Normal.normalize();
        PoseStack.Pose Last = Pose.last();
        Lines.addVertex(Last, (float) Start.x, (float) Start.y, (float) Start.z).setColor(Red, Green, Blue, Alpha).setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
        Lines.addVertex(Last, (float) End.x, (float) End.y, (float) End.z).setColor(Red, Green, Blue, Alpha).setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }
}
