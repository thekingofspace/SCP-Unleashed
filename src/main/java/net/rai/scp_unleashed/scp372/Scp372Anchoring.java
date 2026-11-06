package net.rai.scp_unleashed.scp372;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class Scp372Anchoring {
    public static final int SpawnDistance = 2;
    public static final int[][] SpawnOffsets = {{2, 0}, {2, 1}, {2, -1}, {3, 0}, {1, 1}, {1, -1}, {2, 2}, {2, -2}, {0, 2}, {0, -2}, {3, 1}, {3, -1}};
    public static final double SpawnClearance = 0.6;

    private Scp372Anchoring() {
    }

    public static Optional<Vec3> standingSpot(Level World, BlockPos Column, EntityDimensions Size) {
        for (int Dy = 1; Dy >= -2; Dy--) {
            BlockPos Feet = Column.above(Dy);
            BlockPos Below = Feet.below();
            BlockState Floor = World.getBlockState(Below);
            if (!Floor.isFaceSturdy(World, Below, Direction.UP) && Floor.getCollisionShape(World, Below).isEmpty()) {
                continue;
            }
            if (!World.getBlockState(Feet).getCollisionShape(World, Feet).isEmpty()) {
                continue;
            }
            Vec3 Spot = Vec3.atBottomCenterOf(Feet);
            if (World.noCollision(Size.makeBoundingBox(Spot))) {
                return Optional.of(Spot);
            }
        }
        return Optional.empty();
    }

    public static Optional<Vec3> spawnSpot(Level World, BlockPos Anchor, Direction Facing, EntityDimensions Size) {
        Direction Across = Facing.getClockWise();
        Vec3 Centre = Vec3.atBottomCenterOf(Anchor);
        for (int[] Offset : SpawnOffsets) {
            BlockPos Column = Anchor.relative(Facing, Offset[0]).relative(Across, Offset[1]);
            Optional<Vec3> Spot = standingSpot(World, Column, Size);
            if (Spot.isEmpty()) {
                continue;
            }
            double Dx = Spot.get().x - Centre.x;
            double Dz = Spot.get().z - Centre.z;
            if (Math.sqrt(Dx * Dx + Dz * Dz) <= Scp372Insect.TetherRadius - SpawnClearance) {
                return Spot;
            }
        }
        return Optional.empty();
    }

    public static Optional<Scp372Insect> creature(MinecraftServer Server) {
        Optional<Scp372State.Hold> Held = Scp372State.get(Server).held();
        if (Held.isEmpty()) {
            return Optional.empty();
        }
        ServerLevel World = Server.getLevel(Held.get().Dimension());
        if (World == null) {
            return Optional.empty();
        }
        Entity Found = World.getEntity(Held.get().Creature());
        return Found instanceof Scp372Insect Creature ? Optional.of(Creature) : Optional.empty();
    }

    public static Optional<Scp372AnchorBlockEntity> anchor(MinecraftServer Server) {
        Optional<Scp372State.Hold> Held = Scp372State.get(Server).held();
        if (Held.isEmpty()) {
            return Optional.empty();
        }
        ServerLevel World = Server.getLevel(Held.get().Dimension());
        if (World == null || !World.isLoaded(Held.get().Anchor())) {
            return Optional.empty();
        }
        return World.getBlockEntity(Held.get().Anchor()) instanceof Scp372AnchorBlockEntity Device ? Optional.of(Device) : Optional.empty();
    }

    public static void freed(MinecraftServer Server, long Now) {
        if (Scp372State.get(Server).release(Now + Scp372Glimpses.ResumeDelay)) {
            Scp372Glimpses.resumeAll(Server);
        }
    }

    public static void creatureDied(ServerLevel World, Scp372Insect Creature) {
        MinecraftServer Server = World.getServer();
        Scp372State State = Scp372State.get(Server);
        if (!State.isHeldBy(Creature.getUUID())) {
            return;
        }
        Optional<Scp372AnchorBlockEntity> Device = anchor(Server);
        if (Device.isPresent() && Device.get().holds(Creature.getUUID())) {
            Device.get().release(Scp372AnchorBlockEntity.Release.Died);
            return;
        }
        freed(Server, Server.overworld().getGameTime());
    }

    public static boolean release(MinecraftServer Server) {
        Scp372State State = Scp372State.get(Server);
        if (!State.isHeld()) {
            return false;
        }
        Optional<Scp372AnchorBlockEntity> Device = anchor(Server);
        if (Device.isPresent() && Device.get().isHolding()) {
            Device.get().release(Scp372AnchorBlockEntity.Release.Command);
            return true;
        }
        creature(Server).ifPresent(Scp372Insect::flickerOut);
        freed(Server, Server.overworld().getGameTime());
        return true;
    }
}
