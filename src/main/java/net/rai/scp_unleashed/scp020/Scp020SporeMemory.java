package net.rai.scp_unleashed.scp020;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp020.network.Scp020SporesPayload;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Scp020SporeMemory {
    public static final int Top = 1;
    public static final int Right = 2;
    public static final int Bottom = 4;
    public static final int Left = 8;
    public static final int TopRight = 16;
    public static final int BottomRight = 32;
    public static final int BottomLeft = 64;
    public static final int TopLeft = 128;
    private static final int[] Shapes = shapes();
    public static final int VariantCount = Arrays.stream(Shapes).max().orElse(0) + 1;
    public static final int Quadrants = 4;
    public static final int SpriteCount = VariantCount * Quadrants;
    private final List<Face> Faces = new ArrayList<>();
    private final Set<Face> Present = new HashSet<>();
    private boolean Active;
    private boolean Camera;

    public record Face(BlockPos Host, Direction Side) {
        public Vec3 centre(double Lift) {
            double Out = 0.5 + Lift;
            return Vec3.atCenterOf(Host).add(Side.getStepX() * Out, Side.getStepY() * Out, Side.getStepZ() * Out);
        }

        public Vec3 normal() {
            return new Vec3(Side.getStepX(), Side.getStepY(), Side.getStepZ());
        }
    }

    public static Direction up(Direction Side) {
        return switch (Side) {
            case UP -> Direction.NORTH;
            case DOWN -> Direction.SOUTH;
            default -> Direction.UP;
        };
    }

    public static Direction right(Direction Side) {
        return Side.getAxis() == Direction.Axis.Y ? Direction.EAST : Side.getCounterClockWise();
    }

    public static int canonical(int Mask) {
        int Kept = Mask & (Top | Right | Bottom | Left);
        if ((Mask & TopRight) != 0 && (Mask & (Top | Right)) == (Top | Right)) {
            Kept |= TopRight;
        }
        if ((Mask & BottomRight) != 0 && (Mask & (Bottom | Right)) == (Bottom | Right)) {
            Kept |= BottomRight;
        }
        if ((Mask & BottomLeft) != 0 && (Mask & (Bottom | Left)) == (Bottom | Left)) {
            Kept |= BottomLeft;
        }
        if ((Mask & TopLeft) != 0 && (Mask & (Top | Left)) == (Top | Left)) {
            Kept |= TopLeft;
        }
        return Kept;
    }

    private static int[] shapes() {
        int[] Found = new int[256];
        Arrays.fill(Found, -1);
        int Next = 0;
        for (int Mask = 0; Mask < 256; Mask++) {
            int Shape = canonical(Mask);
            if (Found[Shape] < 0) {
                Found[Shape] = Next++;
            }
        }
        for (int Mask = 0; Mask < 256; Mask++) {
            Found[Mask] = Found[canonical(Mask)];
        }
        return Found;
    }

    public static int variant(int Mask) {
        return Shapes[Mask & 255];
    }

    public static int quadrant(Face Patch) {
        Direction Rightward = right(Patch.Side());
        Direction Upward = up(Patch.Side());
        BlockPos Host = Patch.Host();
        int Across = Host.getX() * Rightward.getStepX() + Host.getY() * Rightward.getStepY() + Host.getZ() * Rightward.getStepZ();
        int Along = Host.getX() * Upward.getStepX() + Host.getY() * Upward.getStepY() + Host.getZ() * Upward.getStepZ();
        return Math.floorMod(Along, 2) * 2 + Math.floorMod(Across, 2);
    }

    public static int sprite(Face Patch, int Variant) {
        return Variant * Quadrants + quadrant(Patch);
    }

    public void apply(Scp020SporesPayload Payload) {
        Faces.clear();
        Present.clear();
        Active = Payload.Active();
        Camera = Payload.Camera();
        if (!Active) {
            return;
        }
        for (int Index = 0; Index < Payload.size(); Index++) {
            BlockPos Host = BlockPos.of(Payload.Hosts()[Index]);
            int Mask = Payload.Masks()[Index] & Scp020Growth.AllFaces;
            for (Direction Side : Direction.values()) {
                if (Scp020Growth.hasFace(Mask, Side)) {
                    Face Patch = new Face(Host, Side);
                    Faces.add(Patch);
                    Present.add(Patch);
                }
            }
        }
    }

    public void clear() {
        Faces.clear();
        Present.clear();
        Active = false;
        Camera = false;
    }

    public boolean isActive() {
        return Active;
    }

    public boolean forCamera() {
        return Camera;
    }

    public boolean shows(boolean OnCamera, boolean Goggled) {
        if (!Active || Faces.isEmpty()) {
            return false;
        }
        return OnCamera ? Camera : Goggled && !Camera;
    }

    public List<Face> faces() {
        return Faces;
    }

    public boolean holds(Face Patch) {
        return Present.contains(Patch);
    }

    public boolean holds(BlockPos Host, Direction Side) {
        return Present.contains(new Face(Host, Side));
    }

    private boolean joins(Face Patch, Direction Toward) {
        BlockPos Host = Patch.Host();
        Direction Side = Patch.Side();
        return holds(Host.relative(Toward), Side) || holds(Host.relative(Side).relative(Toward), Toward.getOpposite()) || holds(Host, Toward);
    }

    private boolean fills(Face Patch, Direction First, Direction Second) {
        BlockPos Host = Patch.Host();
        Direction Side = Patch.Side();
        if (!holds(Host.relative(First), Side) || !holds(Host.relative(Second), Side)) {
            return true;
        }
        return holds(Host.relative(First).relative(Second), Side);
    }

    public int connections(Face Patch) {
        Direction Upward = up(Patch.Side());
        Direction Rightward = right(Patch.Side());
        Direction Downward = Upward.getOpposite();
        Direction Leftward = Rightward.getOpposite();
        int Mask = 0;
        if (joins(Patch, Upward)) {
            Mask |= Top;
        }
        if (joins(Patch, Rightward)) {
            Mask |= Right;
        }
        if (joins(Patch, Downward)) {
            Mask |= Bottom;
        }
        if (joins(Patch, Leftward)) {
            Mask |= Left;
        }
        if ((Mask & (Top | Right)) == (Top | Right) && fills(Patch, Upward, Rightward)) {
            Mask |= TopRight;
        }
        if ((Mask & (Bottom | Right)) == (Bottom | Right) && fills(Patch, Downward, Rightward)) {
            Mask |= BottomRight;
        }
        if ((Mask & (Bottom | Left)) == (Bottom | Left) && fills(Patch, Downward, Leftward)) {
            Mask |= BottomLeft;
        }
        if ((Mask & (Top | Left)) == (Top | Left) && fills(Patch, Upward, Leftward)) {
            Mask |= TopLeft;
        }
        return Mask;
    }
}
