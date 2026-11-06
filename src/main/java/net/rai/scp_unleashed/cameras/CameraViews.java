package net.rai.scp_unleashed.cameras;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.cameras.network.CameraWindowPayload;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class CameraViews {
    private CameraViews() {
    }

    public record View(ServerPlayer Viewer, BlockPos Camera, Vec3 Eye, Vec3 Look) {
    }

    @Nullable
    private static CameraSession session(@Nullable Entity Target) {
        if (!(Target instanceof ServerPlayer User) || User.level().isClientSide) {
            return null;
        }
        CameraSession Session = CameraSessions.get(User.getUUID());
        return Session != null && Session.dimension().equals(User.level().dimension()) ? Session : null;
    }

    public static boolean isViewing(@Nullable Entity Target) {
        return session(Target) != null;
    }

    public static Vec3 eye(Entity Target) {
        CameraSession Session = session(Target);
        return Session == null ? Target.getEyePosition() : Session.eye();
    }

    public static Vec3 origin(Entity Target) {
        CameraSession Session = session(Target);
        return Session == null ? Target.position() : Session.eye();
    }

    public static Vec3 look(Entity Target) {
        CameraSession Session = session(Target);
        return Session == null ? Target.getViewVector(1.0f) : Session.look();
    }

    @Nullable
    public static BlockPos camera(Entity Target) {
        CameraSession Session = session(Target);
        return Session == null ? null : Session.camera();
    }

    public static boolean canSee(LivingEntity Viewer, Entity Target) {
        CameraSession Session = session(Viewer);
        if (Session == null) {
            return Viewer.hasLineOfSight(Target);
        }
        if (Target.level() != Viewer.level()) {
            return false;
        }
        Vec3 To = Target.getEyePosition();
        return Viewer.level().clip(new ClipContext(Session.eye(), To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Viewer)).getType() == HitResult.Type.MISS;
    }

    public static List<View> viewers(Level World) {
        List<View> Found = new ArrayList<>();
        if (World.isClientSide) {
            return Found;
        }
        for (CameraSession Session : CameraSessions.all()) {
            if (!Session.dimension().equals(World.dimension())) {
                continue;
            }
            if (World.getPlayerByUUID(Session.viewer()) instanceof ServerPlayer User && User.isAlive() && !Senses.isBlindfolded(User)) {
                Found.add(new View(User, Session.camera(), Session.eye(), Session.look()));
            }
        }
        return Found;
    }

    public static <T extends Entity> List<T> withViewers(Level World, AABB Area, List<T> Found, Class<T> Kind, Predicate<? super T> Filter) {
        List<T> Result = null;
        for (View Each : viewers(World)) {
            if (!Kind.isInstance(Each.Viewer()) || !Area.contains(Each.Eye())) {
                continue;
            }
            T Viewer = Kind.cast(Each.Viewer());
            if (Found.contains(Viewer) || !Filter.test(Viewer)) {
                continue;
            }
            if (Result == null) {
                Result = new ArrayList<>(Found);
            }
            Result.add(Viewer);
        }
        return Result == null ? Found : Result;
    }

    public static Vec3 trackingOrigin(ServerPlayer User, Vec3 Original, Vec3 Target) {
        CameraSession Session = session(User);
        if (Session == null) {
            return Original;
        }
        Vec3 Lens = Session.eye();
        return flatDistance(Lens, Target) < flatDistance(Original, Target) ? Lens : Original;
    }

    private static double flatDistance(Vec3 From, Vec3 To) {
        double X = From.x - To.x;
        double Z = From.z - To.z;
        return X * X + Z * Z;
    }

    @Nullable
    public static ChunkTrackingView.Positioned lens(ServerPlayer User) {
        CameraSession Session = session(User);
        return Session == null ? null : Session.tracking();
    }

    public static ChunkPos sendCentre(ServerPlayer User, ChunkPos Original) {
        CameraSession Session = session(User);
        return Session == null ? Original : Session.centre();
    }

    public static void sendWindow(ServerPlayer User, @Nullable ChunkPos Lens) {
        if (User.connection != null && User.connection.hasChannel(CameraWindowPayload.PayloadType)) {
            PacketDistributor.sendToPlayer(User, Lens == null ? CameraWindowPayload.Closed : new CameraWindowPayload(true, Lens.x, Lens.z));
        }
    }
}
