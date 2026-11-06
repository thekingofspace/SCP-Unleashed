package net.rai.scp_unleashed.scp096;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.rai.scp_unleashed.cameras.CameraViews;

public final class Scp096Sight {
    public static final double FaceAhead = 0.35;
    public static final double NpcRange = 32.0;
    public static final double NpcFacing = 0.3;
    public static final double NpcCone = 0.5;
    public static final double ValidateRange = 200.0;
    public static final double ValidateFacing = -0.35;
    public static final double CameraSlack = 4.5;

    private Scp096Sight() {
    }

    public static Vec3 faceOf(Scp096ShyGuy Guy) {
        Vec3 Ahead = Vec3.directionFromRotation(0.0f, Guy.getYHeadRot()).scale(FaceAhead);
        return Guy.getEyePosition().add(Ahead);
    }

    public static Vec3 facingOf(Scp096ShyGuy Guy) {
        return Vec3.directionFromRotation(Guy.getXRot(), Guy.getYHeadRot());
    }

    public static boolean clear(ServerLevel World, Vec3 From, Vec3 To) {
        Vec3 Short = To.add(From.subtract(To).normalize().scale(0.05));
        return World.clip(new ClipContext(From, Short, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    public static boolean npcSees(ServerLevel World, LivingEntity Viewer, Vec3 Face, Vec3 Facing) {
        if (!Scp096Persons.canWatch(Viewer)) {
            return false;
        }
        Vec3 Eye = Viewer.getEyePosition();
        Vec3 Toward = Eye.subtract(Face);
        double Distance = Toward.length();
        if (Distance > NpcRange || Distance < 1.0e-3) {
            return false;
        }
        if (Facing.dot(Toward.scale(1.0 / Distance)) < NpcFacing) {
            return false;
        }
        if (Viewer.getViewVector(1.0f).dot(Toward.scale(-1.0 / Distance)) < NpcCone) {
            return false;
        }
        return clear(World, Eye, Face);
    }

    public static boolean plausible(ServerLevel World, ServerPlayer User, Scp096ShyGuy Guy) {
        if (User.level() != World || !User.isAlive() || User.isSpectator() || User.isCreative() || !Scp096Persons.canWatch(User)) {
            return false;
        }
        Vec3 Face = faceOf(Guy);
        Vec3 Eye = CameraViews.eye(User);
        Vec3 Toward = Eye.subtract(Face);
        double Distance = Toward.length();
        if (Distance > ValidateRange || Distance < 1.0e-3) {
            return false;
        }
        if (Guy.isFacingRelevant() && facingOf(Guy).dot(Toward.scale(1.0 / Distance)) < ValidateFacing) {
            return false;
        }
        Vec3 Look = CameraViews.look(User);
        Vec3[] Eyes = {Eye, Eye.subtract(Look.scale(CameraSlack)), Eye.add(Look.scale(CameraSlack))};
        Vec3[] Points = {Face, Guy.getEyePosition(), Guy.position().add(0.0, Guy.getBbHeight() * 0.6, 0.0)};
        for (Vec3 From : Eyes) {
            for (Vec3 Point : Points) {
                if (clear(World, From, Point)) {
                    return true;
                }
            }
        }
        return false;
    }
}
