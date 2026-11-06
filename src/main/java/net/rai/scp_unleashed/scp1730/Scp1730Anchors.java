package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity;
import net.rai.scp_unleashed.scp1730.entity.RealityAnchorField;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp1730Anchors {
    public static final double GrenadeRadius = 5.0;
    public static final int GrenadeTicks = 240;
    public static final double FolderRadius = 14.0;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> Folders = new ConcurrentHashMap<>();

    private Scp1730Anchors() {
    }

    public static void folderRunning(Level World, BlockPos Master, boolean Running) {
        if (World.isClientSide) {
            return;
        }
        Set<BlockPos> Found = Folders.computeIfAbsent(World.dimension(), Key -> ConcurrentHashMap.newKeySet());
        if (Running) {
            Found.add(Master.immutable());
        } else {
            Found.remove(Master);
        }
    }

    public static void clear() {
        Folders.clear();
    }

    public static double gap(Vec3 Centre, AABB Box) {
        double Dx = Math.max(0.0, Math.max(Box.minX - Centre.x, Centre.x - Box.maxX));
        double Dy = Math.max(0.0, Math.max(Box.minY - Centre.y, Centre.y - Box.maxY));
        double Dz = Math.max(0.0, Math.max(Box.minZ - Centre.z, Centre.z - Box.maxZ));
        return Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz);
    }

    public static boolean anchored(Entity Target) {
        return anchored(Target.level(), Target.getBoundingBox());
    }

    public static boolean anchored(Level World, Vec3 Point) {
        return anchored(World, new AABB(Point, Point));
    }

    public static boolean anchored(Level World, AABB Box) {
        return thrown(World, Box) || folded(World, Box);
    }

    public static boolean thrown(Entity Target) {
        return thrown(Target.level(), Target.getBoundingBox());
    }

    public static boolean thrown(Level World, AABB Box) {
        AABB Search = Box.inflate(GrenadeRadius + 1.0);
        for (RealityAnchorField Field : World.getEntitiesOfClass(RealityAnchorField.class, Search, RealityAnchorField::isAlive)) {
            if (gap(Field.position(), Box) <= Field.radius()) {
                return true;
            }
        }
        return false;
    }

    public static boolean folded(Level World, Vec3 Point) {
        return folded(World, new AABB(Point, Point));
    }

    public static boolean folded(Level World, AABB Box) {
        Set<BlockPos> Running = Folders.get(World.dimension());
        if (Running == null || Running.isEmpty()) {
            return false;
        }
        for (BlockPos Master : Running) {
            Vec3 Centre = RealityFolderBlockEntity.fieldCentre(Master);
            if (gap(Centre, Box) <= FolderRadius && World.isLoaded(Master) && World.getBlockEntity(Master) instanceof RealityFolderBlockEntity Folder && Folder.fieldUp()) {
                return true;
            }
        }
        return false;
    }

    public static boolean anchored(Level World, BlockPos Pos) {
        return anchored(World, new AABB(Pos));
    }
}
