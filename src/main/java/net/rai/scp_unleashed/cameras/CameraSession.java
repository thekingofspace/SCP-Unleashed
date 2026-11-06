package net.rai.scp_unleashed.cameras;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class CameraSession {
    private final UUID Viewer;
    private final Vec3 Lock;
    private final ResourceKey<Level> Home;
    private final ChunkPos Body;
    private InteractionHand Hand;
    private ResourceKey<Level> Dimension;
    private BlockPos Camera;
    private ChunkPos Centre;
    private int View;
    private ChunkTrackingView.Positioned Tracking;
    private Vec3 Eye;
    private Vec3 Look;
    private final long Started;

    public CameraSession(UUID Viewer, Vec3 Lock, InteractionHand Hand, ResourceKey<Level> Dimension, BlockPos Camera, ChunkPos Centre, int View, long Started) {
        this.Viewer = Viewer;
        this.Lock = Lock;
        this.Home = Dimension;
        this.Body = new ChunkPos(BlockPos.containing(Lock));
        this.Hand = Hand;
        this.Started = Started;
        retarget(Dimension, Camera, Centre, View);
        Eye = Vec3.atCenterOf(Camera);
        Look = new Vec3(0.0, 0.0, -1.0);
    }

    public void retarget(ResourceKey<Level> NewDimension, BlockPos NewCamera, ChunkPos NewCentre, int NewView) {
        Dimension = NewDimension;
        Camera = NewCamera.immutable();
        Centre = NewCentre;
        View = NewView;
        Tracking = new ChunkTrackingView.Positioned(NewCentre, NewView);
    }

    public void aimFrom(CameraBlockEntity Source) {
        Eye = Source.eye();
        Look = Source.look();
    }

    public UUID viewer() {
        return Viewer;
    }

    public Vec3 lock() {
        return Lock;
    }

    public ResourceKey<Level> home() {
        return Home;
    }

    public ChunkPos body() {
        return Body;
    }

    public InteractionHand hand() {
        return Hand;
    }

    public void setHand(InteractionHand NewHand) {
        Hand = NewHand;
    }

    public ResourceKey<Level> dimension() {
        return Dimension;
    }

    public BlockPos camera() {
        return Camera;
    }

    public ChunkPos centre() {
        return Centre;
    }

    public int view() {
        return View;
    }

    public int ticketRadius() {
        return View + 1;
    }

    public ChunkTrackingView.Positioned tracking() {
        return Tracking;
    }

    public Vec3 eye() {
        return Eye;
    }

    public Vec3 look() {
        return Look;
    }

    public long started() {
        return Started;
    }
}
