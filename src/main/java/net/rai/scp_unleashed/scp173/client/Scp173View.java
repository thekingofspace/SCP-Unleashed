package net.rai.scp_unleashed.scp173.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp173.Scp173Sight;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import net.rai.scp_unleashed.scp173.network.Scp173SightPayload;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp173View {
    public static final double ScanRange = Scp173Sight.MaxRange;
    public static final float NearClip = 0.05f;
    public static final float MinFog = 2.0f;
    public static final int Grid = 3;
    public static final int Rows = 5;
    public static final double Shrink = 0.06;
    public static final int StaleFrames = 2;
    private static final Set<Integer> Visible = new HashSet<>();
    private static long Ticks;
    private static long EvaluatedTick = Long.MIN_VALUE;
    private static long LastSendTick = Long.MIN_VALUE / 2;
    private static List<Integer> LastSent = List.of();
    private static boolean Active;

    private Scp173View() {
    }

    public static boolean ready(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null || !User.isAlive() || User.isSpectator() || User.isSleeping()) {
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || EvaluatedTick == Ticks) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Client.player == null) {
            return;
        }
        EvaluatedTick = Ticks;
        evaluate(Client, Event.getCamera().getPosition(), Event.getModelViewMatrix(), Event.getProjectionMatrix(), Event.getFrustum(), RenderSystem.getShaderFogEnd());
    }

    private static void evaluate(Minecraft Client, Vec3 Camera, Matrix4f ModelView, Matrix4f Projection, Frustum Culling, float FogEnd) {
        ClientLevel World = Client.level;
        Visible.clear();
        boolean Any = false;
        boolean Ready = ready(Client);
        Matrix4f Clip = new Matrix4f(Projection).mul(ModelView);
        float FogLimit = FogEnd > MinFog ? FogEnd : Float.MAX_VALUE;
        EntityRenderDispatcher Dispatcher = Client.getEntityRenderDispatcher();
        for (Entity Found : World.entitiesForRendering()) {
            if (!(Found instanceof Scp173Statue Statue) || !Statue.isAlive() || Statue.distanceToSqr(Camera) > ScanRange * ScanRange) {
                continue;
            }
            Any = true;
            if (!Ready || Culling == null || !Dispatcher.shouldRender(Statue, Culling, Camera.x, Camera.y, Camera.z)) {
                continue;
            }
            if (seen(World, Statue.sightBox(), Camera, Clip, FogLimit)) {
                Visible.add(Statue.getId());
            }
        }
        Active = Any;
    }

    private static boolean seen(ClientLevel World, AABB Box, Vec3 Camera, Matrix4f Clip, float FogLimit) {
        AABB Inner = Box.deflate(Shrink);
        for (int Iy = Rows - 1; Iy >= 0; Iy--) {
            for (int Ix = 0; Ix < Grid; Ix++) {
                for (int Iz = 0; Iz < Grid; Iz++) {
                    double X = Inner.minX + (Inner.maxX - Inner.minX) * Ix / (Grid - 1.0);
                    double Y = Inner.minY + (Inner.maxY - Inner.minY) * Iy / (Rows - 1.0);
                    double Z = Inner.minZ + (Inner.maxZ - Inner.minZ) * Iz / (Grid - 1.0);
                    Vec3 Point = new Vec3(X, Y, Z);
                    if (Point.distanceTo(Camera) > FogLimit || !onScreen(Clip, Camera, Point)) {
                        continue;
                    }
                    if (Scp173Sight.clear(World, Camera, Point)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean onScreen(Matrix4f Clip, Vec3 Camera, Vec3 Point) {
        Vector4f Moved = new Vector4f((float) (Point.x - Camera.x), (float) (Point.y - Camera.y), (float) (Point.z - Camera.z), 1.0f);
        Clip.transform(Moved);
        if (Moved.w <= NearClip) {
            return false;
        }
        float X = Moved.x / Moved.w;
        float Y = Moved.y / Moved.w;
        return X >= -1.0f && X <= 1.0f && Y >= -1.0f && Y <= 1.0f;
    }

    private static boolean anyLoaded(Minecraft Client) {
        if (Client.level == null || Client.player == null) {
            return false;
        }
        for (Entity Found : Client.level.entitiesForRendering()) {
            if (Found instanceof Scp173Statue && Found.distanceToSqr(Client.player) <= ScanRange * ScanRange) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Ticks++;
        if (Client.level == null || Client.player == null || Client.getConnection() == null) {
            reset();
            return;
        }
        if (EvaluatedTick < Ticks - StaleFrames) {
            Visible.clear();
            Active = anyLoaded(Client);
        }
        List<Integer> Current = new ArrayList<>(Visible);
        Current.sort(Integer::compare);
        if (Current.size() > Scp173Sight.MaxReported) {
            Current = new ArrayList<>(Current.subList(0, Scp173Sight.MaxReported));
        }
        if (!Active && LastSent.isEmpty()) {
            return;
        }
        boolean Changed = !Current.equals(LastSent);
        long Since = Ticks - LastSendTick;
        if (Changed && Since >= Scp173Sight.ChangeGap || Since >= Scp173Sight.Heartbeat) {
            PacketDistributor.sendToServer(new Scp173SightPayload(List.copyOf(Current)));
            LastSent = List.copyOf(Current);
            LastSendTick = Ticks;
        }
    }

    private static void reset() {
        Visible.clear();
        LastSent = List.of();
        Active = false;
        EvaluatedTick = Long.MIN_VALUE;
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        reset();
    }
}
