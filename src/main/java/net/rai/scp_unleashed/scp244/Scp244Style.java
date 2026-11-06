package net.rai.scp_unleashed.scp244;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.Scp_unleashed;

public enum Scp244Style {
    Amphora("scp244_vase_a", 14.0f, 1.0f, 2.0f, 0.4f, 5.0f, 14.0f, 32.0f, 3.5f, 90.0f, 16.0f,
            new double[][]{{5, 0, 5, 11, 4, 11}, {4, 4, 4, 12, 9, 12}, {5, 9, 5, 11, 14, 11}, {6, 14, 6, 10, 16, 10}, {3, 8, 7, 13, 13, 9}}),
    Urn("scp244_vase_b", 11.0f, 3.0f, 2.5f, 0.35f, 3.0f, 11.0f, 28.0f, 6.5f, 90.0f, 15.0f,
            new double[][]{{4, 0, 4, 12, 1, 12}, {2, 1, 2, 14, 3, 14}, {1, 3, 1, 15, 7, 15}, {2, 7, 2, 14, 9, 14}, {3, 9, 3, 13, 11, 13}, {2, 11, 2, 14, 12, 14}, {4, 12, 4, 12, 13, 12}, {6, 13, 6, 10, 15, 10},
                    {0, 5, 7, 16, 8, 9}}),
    GingerJar("scp244_vase_c", 11.0f, 2.0f, 3.0f, 0.4f, 5.0f, 11.0f, 25.0f, 5.0f, 0.0f, 15.0f,
            new double[][]{{4, 0, 4, 12, 1, 12}, {3, 1, 3, 13, 2, 13}, {2, 2, 2, 14, 9, 14}, {3, 9, 3, 13, 10, 13}, {5, 10, 5, 11, 11, 11}, {4, 11, 4, 12, 13, 12}, {5, 13, 5, 11, 15, 11}});

    private final String Id;
    private final float MouthHeight;
    private final float MouthHalf;
    private final float LiftPixels;
    private final float LiftShare;
    private final float PivotX;
    private final float PivotY;
    private final float TiltDegrees;
    private final float SlidePixels;
    private final float LidTurn;
    private final float Top;
    private final VoxelShape ShapeAlongX;
    private final VoxelShape ShapeAlongZ;

    Scp244Style(String Id, float MouthHeight, float MouthHalf, float LiftPixels, float LiftShare, float PivotX, float PivotY, float TiltDegrees, float SlidePixels,
                float LidTurn, float Top, double[][] Boxes) {
        this.Id = Id;
        this.MouthHeight = MouthHeight;
        this.MouthHalf = MouthHalf;
        this.LiftPixels = LiftPixels;
        this.LiftShare = LiftShare;
        this.PivotX = PivotX;
        this.PivotY = PivotY;
        this.TiltDegrees = TiltDegrees;
        this.SlidePixels = SlidePixels;
        this.LidTurn = LidTurn;
        this.Top = Top;
        VoxelShape AlongX = Shapes.empty();
        VoxelShape AlongZ = Shapes.empty();
        for (double[] Box : Boxes) {
            AlongX = Shapes.or(AlongX, Block.box(Box[0], Box[1], Box[2], Box[3], Box[4], Box[5]));
            AlongZ = Shapes.or(AlongZ, Block.box(Box[2], Box[1], Box[0], Box[5], Box[4], Box[3]));
        }
        this.ShapeAlongX = AlongX.optimize();
        this.ShapeAlongZ = AlongZ.optimize();
    }

    public String id() {
        return Id;
    }

    public String letter() {
        return Id.substring(Id.length() - 1);
    }

    public ResourceLocation lidModel() {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp244/vase_" + letter() + "_lid");
    }

    public VoxelShape shape(Direction Facing) {
        return Facing.getAxis() == Direction.Axis.X ? ShapeAlongZ : ShapeAlongX;
    }

    public float mouthHeight() {
        return MouthHeight / 16.0f;
    }

    public float mouthHalf() {
        return MouthHalf / 16.0f;
    }

    public float liftPixels() {
        return LiftPixels;
    }

    public float liftShare() {
        return LiftShare;
    }

    public float pivotX() {
        return PivotX;
    }

    public float pivotY() {
        return PivotY;
    }

    public float tiltDegrees() {
        return TiltDegrees;
    }

    public float slidePixels() {
        return SlidePixels;
    }

    public float lidTurn() {
        return LidTurn;
    }

    public float top() {
        return Top / 16.0f;
    }
}
